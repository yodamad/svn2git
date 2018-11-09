import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IMapping } from 'app/shared/model/mapping.model';

type EntityResponseType = HttpResponse<IMapping>;
type EntityArrayResponseType = HttpResponse<IMapping[]>;

@Injectable({ providedIn: 'root' })
export class MappingService {
    private resourceUrl = SERVER_API_URL + 'api/mappings';

    constructor(private http: HttpClient) {}

    create(mapping: IMapping): Observable<EntityResponseType> {
        return this.http.post<IMapping>(this.resourceUrl, mapping, { observe: 'response' });
    }

    update(mapping: IMapping): Observable<EntityResponseType> {
        return this.http.put<IMapping>(this.resourceUrl, mapping, { observe: 'response' });
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http.get<IMapping>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http.get<IMapping[]>(this.resourceUrl, { params: options, observe: 'response' });
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }
}
