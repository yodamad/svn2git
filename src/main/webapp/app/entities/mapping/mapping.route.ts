import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { of } from 'rxjs';
import { map } from 'rxjs/operators';
import { Mapping } from 'app/shared/model/mapping.model';
import { MappingService } from './mapping.service';
import { MappingComponent } from './mapping.component';
import { MappingDetailComponent } from './mapping-detail.component';
import { IMapping } from 'app/shared/model/mapping.model';

@Injectable({ providedIn: 'root' })
export class MappingResolve implements Resolve<IMapping> {
    constructor(private service: MappingService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(map((mapping: HttpResponse<Mapping>) => mapping.body));
        }
        return of(new Mapping());
    }
}

export const mappingRoute: Routes = [
    {
        path: 'mapping',
        component: MappingComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.mapping.home.title'
        }
    },
    {
        path: 'mapping/:id/view',
        component: MappingDetailComponent,
        resolve: {
            mapping: MappingResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.mapping.home.title'
        }
    }
];
