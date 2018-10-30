import { Injectable } from '@angular/core';
import { SERVER_API_URL } from 'app/app.constants';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { IMigration } from 'app/shared/model/migration.model';

type EntityResponseType = HttpResponse<boolean>;
type EntityArrayResponseType = HttpResponse<string[]>;
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

    constructor(private http: HttpClient) {}

    checkUser(name: string): Observable<EntityResponseType> {
        return this.http.get<boolean>(`${this.userUrl}/${name}`, { observe: 'response' }).pipe(map((res: EntityResponseType) => res));
    }

    checkGroup(name: string): Observable<EntityResponseType> {
        return this.http.get<Boolean>(`${this.groupUrl}/${name}`, { observe: 'response' }).pipe(map((res: EntityResponseType) => res));
    }

    checkSvn(name: string): Observable<EntityArrayResponseType> {
        return this.http
            .get<string[]>(`${this.repositoryUrl}/${name}`, { observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => res));
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
}
