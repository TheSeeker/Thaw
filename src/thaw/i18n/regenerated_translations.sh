#!/bin/sh

# Note : Don't use the jdk 1.6.0_02,
#        there is a regression in native2ascii

SRC_TRANSLATION_PREFIX=source
FR_TRANSLATION=thaw_fr.properties

native2ascii $SRC_TRANSLATION_PREFIX.$FR_TRANSLATION $FR_TRANSLATION

