import { Injectable } from '@angular/core';
import { SERVER_API_URL } from 'app/app.constants';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { IMigration } from 'app/shared/model/migration.model';
import { sanitizeUrl } from '@angular/core/src/sanitization/sanitization';
import { el } from '@angular/platform-browser/testing/src/browser_util';

type EntityResponseType = HttpResponse<boolean>;
type EntityStructureResponseType = HttpResponse<SvnStructure>;
type MigrationArrayResponseType = HttpResponse<IMigration[]>;

@Injectable({ providedIn: 'root' })
export class MigrationProcessService {
    private serverUrl = SERVER_API_URL + 'api/';
    private resourceUrl = this.serverUrl + 'gitlab/';
    private userUrl = this.resourceUrl + 'user';
    private groupUrl = this.resourceUrl + 'group';
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

    checkGroup(name: string, url: string, token?: string): Observable<EntityResponseType> {
        const gitlabInfo = new GitlabInfo(url, token);
        return this.http
            .post<Boolean>(`${this.groupUrl}/${name}`, gitlabInfo, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => res));
    }

    createGroup(name: string, url: string, token?: string): Observable<EntityResponseType> {
        const gitlabInfo = new GitlabInfo(url, token);
        return this.http
            .put<Boolean>(`${this.groupUrl}/${name}`, gitlabInfo, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => res));
    }

    checkSvn(name: string, url: string, user: string, password: string): Observable<EntityStructureResponseType> {
        const svnInfo = new SvnInfo(url, user, password);
        return this.http
            .post<SvnStructure>(`${this.repositoryUrl}/${name}`, svnInfo, { observe: 'response' })
            .pipe(map((res: EntityStructureResponseType) => res));
    }

    findActiveMigrations(): Observable<MigrationArrayResponseType> {
        return this.http
            .get<IMigration[]>(`${this.activeMigrationsUrl}`, { observe: 'response' })
            .pipe(map((res: MigrationArrayResponseType) => res));
    }

    findLastMigrations(lastNum: number): Observable<MigrationArrayResponseType> {
        let params = new HttpParams();
        params = params.append('page', '0');
        params = params.append('size', (lastNum as any) as string);
        params = params.append('sort', 'id,desc');
        return this.http
            .get<IMigration[]>(`${this.migrationUrl}`, { observe: 'response', params })
            .pipe(map((res: MigrationArrayResponseType) => res));
    }

    findMigrationByUser(user: string): Observable<MigrationArrayResponseType> {
        return this.http
            .get<IMigration[]>(`${this.userMigrationUrl}${user}`, { observe: 'response' })
            .pipe(map((res: MigrationArrayResponseType) => res));
    }

    findMigrationByGroup(group: string): Observable<MigrationArrayResponseType> {
        return this.http
            .get<IMigration[]>(`${this.groupMigrationUrl}${group}`, { observe: 'response' })
            .pipe(map((res: MigrationArrayResponseType) => res));
    }

    findMigrationByProject(project: string): Observable<MigrationArrayResponseType> {
        const elements: string[] = project.split('/');

        return this.http
            .get<IMigration[]>(`${this.projectMigrationUrl}${elements[elements.length - 1]}`, { observe: 'response' })
            .pipe(map((res: MigrationArrayResponseType) => res));
    }
}

class GitlabInfo {
    constructor(public url: string, public token: string) {}
}

class SvnInfo {
    constructor(public url: string, public user: string, public password: string) {}
}

export class SvnStructure {
    constructor(public name: string, public flat: boolean, public modules: SvnModule[]) {}
}

export class SvnModule {
    constructor(public layoutElements: string[], public name: string, public path: string, public subModules: SvnModule[]) {}
}

export class SvnFlatModule {
    constructor(
        public name: string,
        public path: string,
        public subModules: SvnModule[],
        public expandable: boolean,
        public level: number
    ) {}
}
