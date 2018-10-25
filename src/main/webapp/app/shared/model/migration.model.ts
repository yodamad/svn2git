import { Moment } from 'moment';
import { IMigrationHistory } from 'app/shared/model/migration-history.model';

export interface IMigration {
    id?: number;
    svnGroup?: string;
    svnProject?: string;
    user?: string;
    date?: Moment;
    gitlabGroup?: string;
    gitlabProject?: string;
    status?: string;
    histories?: IMigrationHistory[];
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
        public status?: string,
        public histories?: IMigrationHistory[]
    ) {}
}
