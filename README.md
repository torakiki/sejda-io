Sejda-io (https://www.sejda.org)
=====
![Build Status](https://github.com/torakiki/sejda-io/actions/workflows/build.yml/badge.svg)
[![License](http://img.shields.io/badge/license-APLv2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

A layer of Input/Output classes built on top of java io and nio. Requires JDK-25

Branch 4.x uses the preview feature [Foreign Function & Memory API](https://openjdk.org/jeps/424) to replace
the [Unsafe cleaner workaround](https://github.com/torakiki/sejda-io/blob/1347d7b11273ee72b69c55e0e03bb111b1a67972/src/main/java/org/sejda/io/util/IOUtils.java#L72)
and requires JDK 22 (with preview features enabled)
.

