package com.movtery.zalithlauncher.crashlogs.platform

import com.movtery.zalithlauncher.crashlogs.AbstractAPI

object MirroredAPI: AbstractAPI(
    api = "https://api.mclogs.lemwood.icu/1/log",
    root = "https://mclogs.lemwood.icu"
)