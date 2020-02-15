import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IMigrationRemovedFile } from 'app/shared/model/migration-removed-file.model';

type EntityResponseType = HttpResponse<IMigrationRemovedFile>;
type EntityArrayResponseType = HttpResponse<IMigrationRemovedFile[]>;

@Injectable({ providedIn: 'root' })
export class MigrationRemovedFileService {
    private resourceUrl = SERVER_API_URL + 'api/migration-removed-files';

    constructor(private http: HttpClient) {}

    create(migrationRemovedFile: IMigrationRemovedFile): Observable<EntityResponseType> {
        return this.http.post<IMigrationRemovedFile>(this.resourceUrl, migrationRemovedFile, { observe: 'response' });
    }

    update(migrationRemovedFile: IMigrationRemovedFile): Observable<EntityResponseType> {
        return this.http.put<IMigrationRemovedFile>(this.resourceUrl, migrationRemovedFile, { observe: 'response' });
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http.get<IMigrationRemovedFile>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http.get<IMigrationRemovedFile[]>(this.resourceUrl, { params: options, observe: 'response' });
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }
}
