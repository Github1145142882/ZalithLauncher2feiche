package com.movtery.zalithlauncher.crashlogs.platform

import com.movtery.zalithlauncher.crashlogs.AbstractAPI

object MCLogsAPI: AbstractAPI(
    api = "https://api.mclo.gs/1/log",
    root = "https://mclo.gs"
)