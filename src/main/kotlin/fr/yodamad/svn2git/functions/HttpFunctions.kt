package fr.yodamad.svn2git.functions

import khttp.structures.authorization.BasicAuthorization
import khttp.structures.files.FileLike

fun uploadFileToGitlab(gitlabUrl: String, gitlabToken: String, projectId: Int, projectName: String, version: String, fileName: String, filePath: String) =
     khttp.put(
        url = "$gitlabUrl/api/v4/projects/$projectId/packages/generic/$projectName/$version/$fileName",
        headers = mapOf(
            Pair("PRIVATE-TOKEN", gitlabToken)
        ),
        files = listOf(
            FileLike(fileName, filePath)
        )
    ).statusCode

fun uploadFileToNexus(nexusUrl: String, nexusRepo: String, nexusUser: String, nexusPwd: String,
                      groupName: String, projectName: String, version: String,
                      fileName: String, filePath: String) =
    khttp.put(
        url = "$nexusUrl/repository/$nexusRepo/$groupName/$projectName/$version/$fileName",
        auth = BasicAuthorization(nexusUser, nexusPwd),
        files = listOf(
            FileLike(fileName, filePath)
        )
    ).statusCode
