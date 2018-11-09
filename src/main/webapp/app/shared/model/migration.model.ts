import { Moment } from 'moment';
import { IMigrationHistory } from 'app/shared/model//migration-history.model';
import { IMapping } from 'app/shared/model//mapping.model';

export const enum StatusEnum {
    WAITING = 'WAITING',
    RUNNING = 'RUNNING',
    DONE = 'DONE',
    FAILED = 'FAILED'
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
        public histories?: IMigrationHistory[],
        public mappings?: IMapping[]
    ) {}
}
