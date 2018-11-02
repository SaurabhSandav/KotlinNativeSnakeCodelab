plugins {
    id("org.jetbrains.kotlin.konan").version("0.8.2")
}

konanArtifacts {

    interop("ncurses") {
        defFile("defs/ncurses.def")
    }

    program("Snake") {
        libraries { artifact("ncurses") }
    }
}
