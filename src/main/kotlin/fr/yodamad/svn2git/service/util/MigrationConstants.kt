package fr.yodamad.svn2git.service.util

/** Default ref origin for tags.  */
const val ORIGIN_TAGS = "origin/tags/"

/** Stars to hide sensitive data.  */
const val STARS = "******"

/** Execution error.  */
const val ERROR_CODE = 128

/** Command to copy directory on Windows. */
// /J Copy using unbuffered I/O. Recommended for very large files.
const val WIN_COPY_DIR = "Xcopy /E /I /H /Q"

/** Command to copy directory on none Windows. */
// cp -a /source/. /dest/ ("-a" is recursive "." means files and folders including hidden)
const val COPY_DIR = "cp -a"
