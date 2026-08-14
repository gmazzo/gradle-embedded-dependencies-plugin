package io.github.gmazzo.dependencies.embedded.demo

import local.lib.Lib
import my.org.apache.commons.repackaged.lang3.StringUtils

object Bar {

    val hello = StringUtils.capitalize("hello, world!")

    val libValue = Lib.libValue()

}
