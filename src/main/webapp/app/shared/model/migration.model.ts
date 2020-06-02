import { Moment } from 'moment';
import { IMigrationHistory } from 'app/shared/model//migration-history.model';
import { IMapping } from 'app/shared/model//mapping.model';

export const enum StatusEnum {
    WAITING = 'WAITING',
    RUNNING = 'RUNNING',
    DONE = 'DONE',
    FAILED = 'FAILED',
    IGNORED = 'IGNORED',
    DONE_WITH_WARNINGS = 'DONE_WITH_WARNINGS'
}

export interface IMigration {
    id?: number;
    svnGroup?: string;
    svnProject?: string;
    user?: string;
    date?: Moment;
    gitlabGroup?: string;
    gitlabProject?: string;
    status?: StatusEnum;
    maxFileSize?: string;
    forbiddenFileExtensions?: string;
    gitlabUrl?: string;
    gitlabToken?: string;
    svnUrl?: string;
    svnUser?: string;
    svnPassword?: string;
    trunk?: string;
    branches?: string;
    tags?: string;
    svnHistory?: string;
    tagsToMigrate?: string;
    branchesToMigrate?: string;
    createdTimestamp?: Moment;
    workingDirectory?: string;
    histories?: IMigrationHistory[];
    mappings?: IMapping[];
}

export class Migration implements IMigration {
    constructor(
        public id?: number,
        public svnGroup?: string,
        public svnProject?: string,
        public user?: string,
        public date?: Moment,
        public gitlabGroup?: string,
        public gitlabProject?: string,
        public status?: StatusEnum,
        public maxFileSize?: string,
        public forbiddenFileExtensions?: string,
        public gitlabUrl?: string,
        public gitlabToken?: string,
        public svnUrl?: string,
        public svnUser?: string,
        public svnPassword?: string,
        public trunk?: string,
        public branches?: string,
        public tags?: string,
        public svnHistory?: string,
        public tagsToMigrate?: string,
        public branchesToMigrate?: string,
        public createdTimestamp?: Moment,
        public workingDirectory?: string,
        public histories?: IMigrationHistory[],
        public mappings?: IMapping[]
    ) {}
}
