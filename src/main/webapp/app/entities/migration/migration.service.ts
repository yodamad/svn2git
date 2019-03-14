import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IMigration } from 'app/shared/model/migration.model';
import { IMigrationHistory } from 'app/shared/model/migration-history.model';
import { IMapping } from 'app/shared/model/mapping.model';

type EntityResponseType = HttpResponse<IMigration>;
type EntityArrayResponseType = HttpResponse<IMigration[]>;
type HistoryArrayResponseType = HttpResponse<IMigrationHistory[]>;
type MappingArrayResponseType = HttpResponse<IMapping[]>;

@Injectable({ providedIn: 'root' })
export class MigrationService {
    private resourceUrl = SERVER_API_URL + 'api/migrations';

    constructor(private http: HttpClient) {}

    create(migration: IMigration): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(migration);
        return this.http
            .post<IMigration>(this.resourceUrl, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    retry(id: number, forceClean: boolean): Observable<EntityResponseType> {
        return this.http
            .post<IMigration>(`${this.resourceUrl}/${id}/retry`, forceClean, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => res));
    }

    update(migration: IMigration): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(migration);
        return this.http
            .put<IMigration>(this.resourceUrl, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http
            .get<IMigration>(`${this.resourceUrl}/${id}`, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    findHistories(id: number): Observable<HistoryArrayResponseType> {
        return this.http
            .get<IMigrationHistory[]>(`${this.resourceUrl}/${id}/histories`, { observe: 'response' })
            .pipe(map((res: HistoryArrayResponseType) => res));
    }

    findMappings(id: number): Observable<MappingArrayResponseType> {
        return this.http
            .get<IMapping[]>(`${this.resourceUrl}/${id}/mappings`, { observe: 'response' })
            .pipe(map((res: MappingArrayResponseType) => res));
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http
            .get<IMigration[]>(this.resourceUrl, { params: options, observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    private convertDateFromClient(migration: IMigration): IMigration {
        const copy: IMigration = Object.assign({}, migration, {
            date: migration.date != null && migration.date.isValid() ? migration.date.format(DATE_FORMAT) : null
        });
        return copy;
    }

    private convertDateFromServer(res: EntityResponseType): EntityResponseType {
        res.body.date = res.body.date != null ? moment(res.body.date) : null;
        return res;
    }

    private convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
        res.body.forEach((migration: IMigration) => {
            migration.date = migration.date != null ? moment(migration.date) : null;
        });
        return res;
    }
}
