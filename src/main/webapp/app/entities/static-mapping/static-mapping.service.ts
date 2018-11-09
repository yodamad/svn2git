import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IStaticMapping } from 'app/shared/model/static-mapping.model';

type EntityResponseType = HttpResponse<IStaticMapping>;
type EntityArrayResponseType = HttpResponse<IStaticMapping[]>;

@Injectable({ providedIn: 'root' })
export class StaticMappingService {
    private resourceUrl = SERVER_API_URL + 'api/static-mappings';

    constructor(private http: HttpClient) {}

    create(staticMapping: IStaticMapping): Observable<EntityResponseType> {
        return this.http.post<IStaticMapping>(this.resourceUrl, staticMapping, { observe: 'response' });
    }

    update(staticMapping: IStaticMapping): Observable<EntityResponseType> {
        return this.http.put<IStaticMapping>(this.resourceUrl, staticMapping, { observe: 'response' });
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http.get<IStaticMapping>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http.get<IStaticMapping[]>(this.resourceUrl, { params: options, observe: 'response' });
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }
}
