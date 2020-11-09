export interface IMigrationFilter {
    pageIndex?: number;
    pageSize?: number;
    user?: string;
    group?: string;
    project?: string;
}

export class MigrationFilter implements IMigrationFilter {
    constructor(
        public pageIndex?: number,
        public pageSize?: number,
        public user?: string,
        public group?: string,
        public project?: string
    ) {}
}
