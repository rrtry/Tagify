#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <sstream>
#include <chrono>
#include <chromaprint.h>
#include "audio/ffmpeg_audio_reader.h"
#include "utils/scope_exit.h"
#include "fpcalc_result.h"
#include <jni.h>
#include <string>
#include <memory>
#include <vector>
#include <android/log.h>
#include <stdlib.h>

#define LOGI(...)   __android_log_print((int)ANDROID_LOG_INFO, "CHROMAPRINT", __VA_ARGS__)

extern std::string format(const char *format, ...);

using namespace chromaprint;

enum Format {
    TEXT = 0,
    JSON,
    PLAIN,
};

static Format g_format = TEXT;
static char *g_input_format = nullptr;
static int g_input_channels = 0;
static int g_input_sample_rate = 0;
static double g_max_duration = 120;
static double g_max_chunk_duration = 0;
static bool g_overlap = false;
static bool g_raw = false;
static bool g_abs_ts = false;
static bool g_ignore_errors = false;
static ChromaprintAlgorithm g_algorithm = CHROMAPRINT_ALGORITHM_DEFAULT;


const char *g_help =
        "Usage: %s [OPTIONS] FILE [FILE...]\n"
        "\n"
        "Generate fingerprints from audio files/streams.\n"
        "\n"
        "Options:\n"
        "  -format NAME   Set the input format name\n"
        "  -rate NUM      Set the sample rate of the input audio\n"
        "  -channels NUM  Set the number of channels in the input audio\n"
        "  -length SECS   Restrict the duration of the processed input audio (default 120)\n"
        "  -chunk SECS    Split the input audio into chunks of this duration\n"
        "  -algorithm NUM Set the algorigthm method (default 2)\n"
        "  -overlap       Overlap the chunks slightly to make sure audio on the edges is fingerprinted\n"
        "  -ts            Output UNIX timestamps for chunked results, useful when fingerprinting real-time audio stream\n"
        "  -raw           Output fingerprints in the uncompressed format\n"
        "  -json          Print the output in JSON format\n"
        "  -text          Print the output in text format\n"
        "  -plain         Print the just the fingerprint in text format\n"
        "  -version       Print version information\n";

static void ParseOptions(FpcalcResult &result, int &argc, char **argv) {
    int j = 1;
    for (int i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "--")) {
            while (++i < argc) {
                argv[j++] = argv[i];
            }
        } else if ((!strcmp(argv[i], "-format") || !strcmp(argv[i], "-f")) && i + 1 < argc) {
            g_input_format = argv[++i];
        } else if ((!strcmp(argv[i], "-channels") || !strcmp(argv[i], "-c")) && i + 1 < argc) {
            auto value = atoi(argv[i + 1]);
            if (value > 0) {
                g_input_channels = value;
            } else {
                result.error = format("ERROR: The argument for %s must be a non-zero positive number\n", argv[i]);
                return;
            }
            i++;
        } else if ((!strcmp(argv[i], "-rate") || !strcmp(argv[i], "-r")) && i + 1 < argc) {
            auto value = atoi(argv[i + 1]);
            if (value >= 0) {
                g_input_sample_rate = value;
            } else {
                result.error = format("ERROR: The argument for %s must be a positive number\n", argv[i]);
                return;
            }
            i++;
        } else if ((!strcmp(argv[i], "-length") || !strcmp(argv[i], "-t")) && i + 1 < argc) {
            auto value = atof(argv[i + 1]);
            if (value >= 0) {
                g_max_duration = value;
            } else {
                result.error = format("ERROR: The argument for %s must be a positive number\n", argv[i]);
                return;
            }
            i++;
        } else if (!strcmp(argv[i], "-chunk") && i + 1 < argc) {
            auto value = atof(argv[i + 1]);
            if (value >= 0) {
                g_max_chunk_duration = value;
            } else {
                result.error = format("ERROR: The argument for %s must be a positive number\n", argv[i]);
                return;
            }
            i++;
        } else if ((!strcmp(argv[i], "-algorithm") || !strcmp(argv[i], "-a")) && i + 1 < argc) {
            auto value = atoi(argv[i + 1]);
            if (value >= 1 && value <= 5) {
                g_algorithm = (ChromaprintAlgorithm) (value - 1);
            } else {
                result.error = format("ERROR: The argument for %s must be 1 - 5\n", argv[i]);
                return;
            }
            i++;
        } else if (!strcmp(argv[i], "-text")) {
            g_format = TEXT;
        } else if (!strcmp(argv[i], "-json")) {
            g_format = JSON;
        } else if (!strcmp(argv[i], "-plain")) {
            g_format = PLAIN;
        } else if (!strcmp(argv[i], "-overlap")) {
            g_overlap = true;
        } else if (!strcmp(argv[i], "-ts")) {
            g_abs_ts = true;
        } else if (!strcmp(argv[i], "-raw")) {
            g_raw = true;
        } else if (!strcmp(argv[i], "-ignore-errors")) {
            g_ignore_errors = true;
        } else if (!strcmp(argv[i], "-v") || !strcmp(argv[i], "-version")) {
            return;
        } else if (!strcmp(argv[i], "-h") || !strcmp(argv[i], "-help") ||
                   !strcmp(argv[i], "--help")) {
            return;
        } else {
            const auto len = strlen(argv[i]);
            if (len > 1 && argv[i][0] == '-') {
                result.error = format("ERROR: Unknown option %s\n", argv[i]);
                return;
            } else {
                argv[j++] = argv[i];
            }
        }
    }
    if (j < 2) {
        result.error = "ERROR: No input files\n";
        return;
    }
    argc = j;
}

void PrintResult(
        FpcalcResult &result,
        ChromaprintContext *ctx,
        FFmpegAudioReader &reader,
        bool first,
        double timestamp,
        double duration
) {
    std::string tmp_fp;
    const char *fp;
    bool dealloc_fp = false;

    int size;
    if (!chromaprint_get_raw_fingerprint_size(ctx, &size)) {
        result.error = "ERROR: Could not get the fingerprinting size\n";
        return;
    }
    if (size <= 0) {
        if (first) {
            result.error = "ERROR: Empty fingerprint\n";
            return;
        }
        return;
    }

    if (g_raw) {
        std::stringstream ss;
        uint32_t *raw_fp_data = nullptr;
        int raw_fp_size = 0;
        if (!chromaprint_get_raw_fingerprint(ctx, &raw_fp_data, &raw_fp_size)) {
            result.error = "ERROR: Could not get the fingerprinting\n";
            return;
        }
        SCOPE_EXIT(chromaprint_dealloc(raw_fp_data));
        for (int i = 0; i < raw_fp_size; i++) {
            if (i > 0) {
                ss << ',';
            }
            ss << raw_fp_data[i];
        }
        tmp_fp = ss.str();
        fp = tmp_fp.c_str();
    } else {
        char *tmp_fp2;
        if (!chromaprint_get_fingerprint(ctx, &tmp_fp2)) {
            result.error = "ERROR: Could not get the fingerprinting\n";
            return;
        }
        fp = tmp_fp2;
        dealloc_fp = true;
    }
    SCOPE_EXIT(if (dealloc_fp) { chromaprint_dealloc((void *) fp); });

    if (g_max_chunk_duration == 0) {
        duration = reader.GetDuration();
        if (duration < 0.0) {
            duration = 0.0;
        } else {
            duration /= 1000.0;
        }
    }

    switch (g_format) {
        case TEXT:
            if (!first) {
                result.fingerprint += "\n";
            }
            if (g_abs_ts) {
                result.fingerprint += format("TIMESTAMP=%.2f\n", timestamp);
            }
            result.fingerprint += format("DURATION=%d\nFINGERPRINT=%s\n", int(duration), fp);
            break;
        case JSON:
            if (g_max_chunk_duration != 0) {
                if (g_raw) {
                    result.fingerprint += format("{\"timestamp\": %.2f, \"duration\": %.2f, \"fingerprint\": [%s]}\n",
                                                 timestamp, duration, fp);
                } else {
                    result.fingerprint += format("{\"timestamp\": %.2f, \"duration\": %.2f, \"fingerprint\": \"%s\"}\n",
                                                 timestamp, duration, fp);
                }
            } else {
                if (g_raw) {
                    result.fingerprint += format("{\"duration\": %.2f, \"fingerprint\": [%s]}\n", duration, fp);
                } else {
                    result.fingerprint += format("{\"duration\": %.2f, \"fingerprint\": \"%s\"}\n", duration, fp);
                }
            }
            break;
        case PLAIN:
            result.fingerprint += fp;
            break;
    }
}

double GetCurrentTimestamp() {
    const auto now = std::chrono::system_clock::now();
    const auto usec = std::chrono::duration_cast<std::chrono::microseconds>(now.time_since_epoch());
    return usec.count() / 1000000.0;
}

void ProcessFile(FpcalcResult &result,ChromaprintContext *ctx, FFmpegAudioReader &reader, const char *file_name) {
    double ts = 0.0;
    if (g_abs_ts) {
        ts = GetCurrentTimestamp();
    }

    if (!strcmp(file_name, "-")) {
        file_name = "pipe:0";
    }

    if (!reader.Open(file_name)) {
        result.error = format("ERROR: %s\n", reader.GetError().c_str());
        return;
    }

    if (!chromaprint_start(ctx, reader.GetSampleRate(), reader.GetChannels())) {
        result.error = "ERROR: Could not initialize the fingerprinting process\n";
        return;
    }

    size_t stream_size = 0;
    const size_t stream_limit = g_max_duration * reader.GetSampleRate();

    size_t chunk_size = 0;
    const size_t chunk_limit = g_max_chunk_duration * reader.GetSampleRate();

    size_t extra_chunk_limit = 0;
    double overlap = 0.0;
    if (chunk_limit > 0 && g_overlap) {
        extra_chunk_limit = chromaprint_get_delay(ctx);
        overlap = chromaprint_get_delay_ms(ctx) / 1000.0;
    }

    bool first_chunk = true;
    bool read_failed = false;
    bool got_results = false;

    while (!reader.IsFinished()) {
        const int16_t *frame_data = nullptr;
        size_t frame_size = 0;
        if (!reader.Read(&frame_data, &frame_size)) {
            result.error = format("ERROR: %s\n", reader.GetError().c_str());
            read_failed = true;
            break;
        }

        bool stream_done = false;
        if (stream_limit > 0) {
            const auto remaining = stream_limit - stream_size;
            if (frame_size > remaining) {
                frame_size = remaining;
                stream_done = true;
            }
        }
        stream_size += frame_size;

        if (frame_size == 0) {
            if (stream_done) {
                break;
            } else {
                continue;
            }
        }

        bool chunk_done = false;
        size_t first_part_size = frame_size;
        if (chunk_limit > 0) {
            const auto remaining = chunk_limit + extra_chunk_limit - chunk_size;
            if (first_part_size > remaining) {
                first_part_size = remaining;
                chunk_done = true;
            }
        }

        if (!chromaprint_feed(ctx, frame_data, first_part_size * reader.GetChannels())) {
            result.error = "ERROR: Could not process audio data\n";
            return;
        }

        chunk_size += first_part_size;

        if (chunk_done) {
            if (!chromaprint_finish(ctx)) {
                result.error = "ERROR: Could not finish the fingerprinting process\n";
                return;
            }

            const auto chunk_duration =
                    (chunk_size - extra_chunk_limit) * 1.0 / reader.GetSampleRate() + overlap;
            PrintResult(result, ctx, reader, first_chunk, ts, chunk_duration);
            got_results = true;

            if (g_abs_ts) {
                ts = GetCurrentTimestamp();
            } else {
                ts += chunk_duration;
            }

            if (g_overlap) {
                if (!chromaprint_clear_fingerprint(ctx)) {
                    result.error = "ERROR: Could not initialize the fingerprinting process\n";
                    return;
                }
                ts -= overlap;
            } else {
                if (!chromaprint_start(ctx, reader.GetSampleRate(), reader.GetChannels())) {
                    result.error = "ERROR: Could not initialize the fingerprinting process\n";
                    return;
                }
            }

            if (first_chunk) {
                extra_chunk_limit = 0;
                first_chunk = false;
            }

            chunk_size = 0;
        }

        frame_data += first_part_size * reader.GetChannels();
        frame_size -= first_part_size;

        if (frame_size > 0) {
            if (!chromaprint_feed(ctx, frame_data, frame_size * reader.GetChannels())) {
                result.error = "ERROR: Could not process audio data\n";
                return;
            }
        }

        chunk_size += frame_size;

        if (stream_done) {
            break;
        }
    }

    if (!chromaprint_finish(ctx)) {
        result.error = "ERROR: Could not finish the fingerprinting process\n";
        return;
    }

    if (chunk_size > 0) {
        const auto chunk_duration =
                (chunk_size - extra_chunk_limit) * 1.0 / reader.GetSampleRate() + overlap;
        PrintResult(result, ctx, reader, first_chunk, ts, chunk_duration);
        got_results = true;
        result.success = true;
    } else if (first_chunk) {
        result.error = "ERROR: Not enough audio data\n";
        return;
    }

    if (!g_ignore_errors) {
        if (read_failed) {
            result.error = "ERROR: Read failed\n";
        }
    }
}

int fpcalc_main(FpcalcResult &result, int &argc, char **argv) {

    ParseOptions(result, argc, argv);
    FFmpegAudioReader reader;

    if (g_input_format) {
        if (!reader.SetInputFormat(g_input_format)) {
            result.error = "ERROR: Invalid format\n";
            return 2;
        }
    }
    if (g_input_channels) {
        if (!reader.SetInputChannels(g_input_channels)) {
            result.error = "ERROR: Invalid number of channels\n";
            return 2;
        }
    }
    if (g_input_sample_rate) {
        if (!reader.SetInputSampleRate(g_input_sample_rate)) {
            result.error = "ERROR: Invalid sample rate\n";
            return 2;
        }
    }

    ChromaprintContext *chromaprint_ctx = chromaprint_new(g_algorithm);
    SCOPE_EXIT(chromaprint_free(chromaprint_ctx));

    reader.SetOutputChannels(chromaprint_get_num_channels(chromaprint_ctx));
    reader.SetOutputSampleRate(chromaprint_get_sample_rate(chromaprint_ctx));
    for (int i = 1; i < argc; i++) {
        ProcessFile(result, chromaprint_ctx, reader, argv[i]);
    }
    return 0;
}
