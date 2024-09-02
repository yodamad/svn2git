import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { of } from 'rxjs';
import { map } from 'rxjs/operators';
import { StaticExtension } from 'app/shared/model/static-extension.model';
import { StaticExtensionService } from './static-extension.service';
import { StaticExtensionComponent } from './static-extension.component';
import { StaticExtensionDetailComponent } from './static-extension-detail.component';
import { StaticExtensionUpdateComponent } from './static-extension-update.component';
import { StaticExtensionDeletePopupComponent } from './static-extension-delete-dialog.component';
import { IStaticExtension } from 'app/shared/model/static-extension.model';

@Injectable({ providedIn: 'root' })
export class StaticExtensionResolve implements Resolve<IStaticExtension> {
    constructor(private service: StaticExtensionService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(map((staticExtension: HttpResponse<StaticExtension>) => staticExtension.body));
        }
        return of(new StaticExtension());
    }
}

export const staticExtensionRoute: Routes = [
    {
        path: 'static-extension',
        component: StaticExtensionComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.staticExtension.home.title'
        }
    },
    {
        path: 'static-extension/:id/view',
        component: StaticExtensionDetailComponent,
        resolve: {
            staticExtension: StaticExtensionResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.staticExtension.home.title'
        }
    },
    {
        path: 'static-extension/new',
        component: StaticExtensionUpdateComponent,
        resolve: {
            staticExtension: StaticExtensionResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.staticExtension.home.title'
        }
    },
    {
        path: 'static-extension/:id/edit',
        component: StaticExtensionUpdateComponent,
        resolve: {
            staticExtension: StaticExtensionResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.staticExtension.home.title'
        }
    }
];

export const staticExtensionPopupRoute: Routes = [
    {
        path: 'static-extension/:id/delete',
        component: StaticExtensionDeletePopupComponent,
        resolve: {
            staticExtension: StaticExtensionResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.staticExtension.home.title'
        },
        outlet: 'popup'
    }
];
