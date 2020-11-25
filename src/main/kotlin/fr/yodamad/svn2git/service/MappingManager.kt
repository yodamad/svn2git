package fr.yodamad.svn2git.service

import fr.yodamad.svn2git.domain.Mapping
import fr.yodamad.svn2git.repository.MappingRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
open class MappingManager(val historyMgr: HistoryManager,
                          val mappingRepository: MappingRepository) {

    /**
     * return list of svnDirectories to delete from a Set of Mappings
     *
     * @param migrationId
     * @return
     */
    open fun getSvnDirectoryDeleteList(migrationId: Long): List<String> {
        val mappings = mappingRepository.findByMigrationAndSvnDirectoryDelete(migrationId, true)
        val svnDirectoryDeleteList: MutableList<String> = ArrayList()
        val it: Iterator<Mapping> = mappings.iterator()
        while (it.hasNext()) {
            val mp = it.next()
            if (mp.isSvnDirectoryDelete) {
                svnDirectoryDeleteList.add(mp.svnDirectory)
            }
        }
        return svnDirectoryDeleteList
    }
}
