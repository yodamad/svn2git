import { Injectable } from '@angular/core';
import { SERVER_API_URL } from 'app/app.constants';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

type EntityResponseType = HttpResponse<boolean>;
type EntityArrayResponseType = HttpResponse<string[]>;

@Injectable({ providedIn: 'root' })
export class MigrationProcessService {
    private resourceUrl = SERVER_API_URL + 'api/gitlab/';
    private userUrl = this.resourceUrl + 'user';
    private groupUrl = this.resourceUrl + 'group';
    private svnUrl = SERVER_API_URL + 'api/svn/';
    private repositoryUrl = this.svnUrl + 'repository';

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
}
