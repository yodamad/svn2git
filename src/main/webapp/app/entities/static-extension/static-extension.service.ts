import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IStaticExtension } from 'app/shared/model/static-extension.model';

type EntityResponseType = HttpResponse<IStaticExtension>;
type EntityArrayResponseType = HttpResponse<IStaticExtension[]>;

@Injectable({ providedIn: 'root' })
export class StaticExtensionService {
    private resourceUrl = SERVER_API_URL + 'api/static-extensions';

    constructor(private http: HttpClient) {}

    create(staticExtension: IStaticExtension): Observable<EntityResponseType> {
        return this.http.post<IStaticExtension>(this.resourceUrl, staticExtension, { observe: 'response' });
    }

    update(staticExtension: IStaticExtension): Observable<EntityResponseType> {
        return this.http.put<IStaticExtension>(this.resourceUrl, staticExtension, { observe: 'response' });
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http.get<IStaticExtension>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        let url = this.resourceUrl;
        if (req && req.name) {
            url += `/repository/${req.name}`;
        }
        return this.http.get<IStaticExtension[]>(url, { observe: 'response' });
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }
}
