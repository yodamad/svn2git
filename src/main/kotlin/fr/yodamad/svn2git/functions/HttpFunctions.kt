package fr.yodamad.svn2git.functions

import khttp.structures.files.FileLike

fun uploadFile(gitlabUrl: String, gitlabToken: String, projectId: Int, projectName: String, version: String, fileName: String, filePath: String) =
     khttp.put(
        url = "$gitlabUrl/api/v4/projects/$projectId/packages/generic/$projectName/$version/$fileName",
        headers = mapOf(
            Pair("PRIVATE-TOKEN", gitlabToken)
        ),
        files = listOf(
            FileLike(fileName, filePath)
        )
    ).statusCode
