import { Injectable } from '@angular/core';
import { SERVER_API_URL } from 'app/app.constants';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { IMigration } from 'app/shared/model/migration.model';
import { MigrationFilter } from 'app/shared/model/migration-filter.model';

type EntityResponseType = HttpResponse<boolean>;
type EntityStructureResponseType = HttpResponse<SvnStructure>;
type MigrationArrayResponseType = HttpResponse<IMigration[]>;

@Injectable({ providedIn: 'root' })
export class MigrationProcessService {
    private serverUrl = SERVER_API_URL + 'api/';
    private resourceUrl = this.serverUrl + 'gitlab/';
    private userUrl = this.resourceUrl + 'user';
    private groupUrl = this.resourceUrl + 'group';
    private projectUrl = this.resourceUrl + 'project';
    private svnUrl = this.serverUrl + 'svn/';
    private repositoryUrl = this.svnUrl + 'repository';
    private migrationUrl = this.serverUrl + 'migrations';
    private userMigrationUrl = this.migrationUrl + '/user/';
    private groupMigrationUrl = this.migrationUrl + '/group/';
    private projectMigrationUrl = this.migrationUrl + '/project/';
    private activeMigrationsUrl = this.migrationUrl + '/active';

    constructor(private http: HttpClient) {}

    checkUser(name: string, url: string, token?: string): Observable<EntityResponseType> {
        const gitlabInfo = new GitlabInfo(url, token);
        return this.http
            .post<boolean>(`${this.userUrl}/${name}`, gitlabInfo, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => res));
    }

    checkGroup(name: string, userName: string, url: string, token?: string): Observable<EntityResponseType> {
        const gitlabInfo = new GitlabInfo(url, token, name);
        return this.http
            .post<Boolean>(`${this.groupUrl}/members/${userName}`, gitlabInfo, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => res));
    }

    checkProject(project: string, url: string, token?: string): Observable<EntityResponseType> {
        const gitlabInfo = new GitlabInfo(url, token);
        gitlabInfo.additionalData = project;
        return this.http.post(`${this.projectUrl}`, gitlabInfo, { observe: 'response' }).pipe(map((res: EntityResponseType) => res));
    }

    createGroup(name: string, url: string, token?: string): Observable<EntityResponseType> {
        const gitlabInfo = new GitlabInfo(url, token);
        return this.http
            .put<boolean>(`${this.groupUrl}/${name}`, gitlabInfo, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => res));
    }

    checkSvn(name: string, url: string, user: string, password: string, depth: number): Observable<EntityStructureResponseType> {
        const svnInfo = new SvnInfo(url, user, password);
        return this.http.post<SvnStructure>(`${this.repositoryUrl}/${name}?depth=${depth}`, svnInfo, { observe: 'response' }).pipe(
            map((res: EntityStructureResponseType) => res),
            catchError(err => {
                console.log(err);
                throw err;
            })
        );
    }

    findActiveMigrations(): Observable<MigrationArrayResponseType> {
        return this.http
            .get<IMigration[]>(`${this.activeMigrationsUrl}`, { observe: 'response' })
            .pipe(map((res: MigrationArrayResponseType) => res));
    }

    findMigrations(filter: MigrationFilter): Observable<MigrationArrayResponseType> {
        let params = new HttpParams();
        params = params.append('page', filter.pageIndex.toString());
        params = params.append('size', filter.pageSize.toString());
        params = params.append('sort', 'id,desc');
        let url = this.migrationUrl;
        if (filter.user.length) {
            url = this.userMigrationUrl + filter.user;
        }
        if (filter.group.length) {
            url = this.groupMigrationUrl + filter.group;
        }
        if (filter.project.length) {
            url = this.projectMigrationUrl + filter.project;
        }
        return this.http.get<IMigration[]>(`${url}`, { observe: 'response', params });
    }
}

class GitlabInfo {
    constructor(public url: string, public token: string, public additionalData = '') {}
}

class SvnInfo {
    constructor(public url: string, public user: string, public password: string) {}
}

export class SvnStructure {
    constructor(public name: string, public flat: boolean, public root: boolean, public modules: SvnModule[]) {}
}

export class SvnModule {
    constructor(
        public layoutElements: string[],
        public name: string,
        public path: string,
        public subModules: SvnModule[],
        public flat: boolean
    ) {}
}

export class SvnFlatModule {
    constructor(
        public name: string,
        public path: string,
        public subModules: SvnModule[],
        public expandable: boolean,
        public level: number,
        public flat: boolean
    ) {}
}
