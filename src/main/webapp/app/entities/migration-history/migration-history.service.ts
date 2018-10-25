import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IMigrationHistory } from 'app/shared/model/migration-history.model';

type EntityResponseType = HttpResponse<IMigrationHistory>;
type EntityArrayResponseType = HttpResponse<IMigrationHistory[]>;

@Injectable({ providedIn: 'root' })
export class MigrationHistoryService {
    private resourceUrl = SERVER_API_URL + 'api/migration-histories';

    constructor(private http: HttpClient) {}

    create(migrationHistory: IMigrationHistory): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(migrationHistory);
        return this.http
            .post<IMigrationHistory>(this.resourceUrl, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    update(migrationHistory: IMigrationHistory): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(migrationHistory);
        return this.http
            .put<IMigrationHistory>(this.resourceUrl, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http
            .get<IMigrationHistory>(`${this.resourceUrl}/${id}`, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http
            .get<IMigrationHistory[]>(this.resourceUrl, { params: options, observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    private convertDateFromClient(migrationHistory: IMigrationHistory): IMigrationHistory {
        const copy: IMigrationHistory = Object.assign({}, migrationHistory, {
            date: migrationHistory.date != null && migrationHistory.date.isValid() ? migrationHistory.date.toJSON() : null
        });
        return copy;
    }

    private convertDateFromServer(res: EntityResponseType): EntityResponseType {
        res.body.date = res.body.date != null ? moment(res.body.date) : null;
        return res;
    }

    private convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
        res.body.forEach((migrationHistory: IMigrationHistory) => {
            migrationHistory.date = migrationHistory.date != null ? moment(migrationHistory.date) : null;
        });
        return res;
    }
}
