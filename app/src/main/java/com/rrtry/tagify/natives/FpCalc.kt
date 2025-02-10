package com.rrtry.tagify.natives

object FpCalc {

    init {
        System.loadLibrary("fpcalc")
    }

    external fun exec(args: Array<String>): FpCalcResult
}