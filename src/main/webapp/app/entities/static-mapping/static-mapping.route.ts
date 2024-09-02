import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { of } from 'rxjs';
import { map } from 'rxjs/operators';
import { StaticMapping } from 'app/shared/model/static-mapping.model';
import { StaticMappingService } from './static-mapping.service';
import { StaticMappingComponent } from './static-mapping.component';
import { StaticMappingDetailComponent } from './static-mapping-detail.component';
import { StaticMappingUpdateComponent } from './static-mapping-update.component';
import { StaticMappingDeletePopupComponent } from './static-mapping-delete-dialog.component';
import { IStaticMapping } from 'app/shared/model/static-mapping.model';

@Injectable({ providedIn: 'root' })
export class StaticMappingResolve implements Resolve<IStaticMapping> {
    constructor(private service: StaticMappingService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(map((staticMapping: HttpResponse<StaticMapping>) => staticMapping.body));
        }
        return of(new StaticMapping());
    }
}

export const staticMappingRoute: Routes = [
    {
        path: 'static-mapping',
        component: StaticMappingComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.staticMapping.home.title'
        }
    },
    {
        path: 'static-mapping/:id/view',
        component: StaticMappingDetailComponent,
        resolve: {
            staticMapping: StaticMappingResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.staticMapping.home.title'
        }
    },
    {
        path: 'static-mapping/new',
        component: StaticMappingUpdateComponent,
        resolve: {
            staticMapping: StaticMappingResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.staticMapping.home.title'
        }
    },
    {
        path: 'static-mapping/:id/edit',
        component: StaticMappingUpdateComponent,
        resolve: {
            staticMapping: StaticMappingResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.staticMapping.home.title'
        }
    }
];

export const staticMappingPopupRoute: Routes = [
    {
        path: 'static-mapping/:id/delete',
        component: StaticMappingDeletePopupComponent,
        resolve: {
            staticMapping: StaticMappingResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.staticMapping.home.title'
        },
        outlet: 'popup'
    }
];
