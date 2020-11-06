import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core';
import { of } from 'rxjs';
import { map } from 'rxjs/operators';
import { Migration } from 'app/shared/model/migration.model';
import { MigrationService } from './migration.service';
import { MigrationComponent } from './migration.component';
import { MigrationUpdateComponent } from './migration-update.component';
import { MigrationDeletePopupComponent } from './migration-delete-dialog.component';
import { IMigration } from 'app/shared/model/migration.model';
import { MigrationDetailComponent } from 'app/entities/migration/migration-detail.component';

@Injectable({ providedIn: 'root' })
export class MigrationResolve implements Resolve<IMigration> {
    constructor(private service: MigrationService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(map((migration: HttpResponse<Migration>) => migration.body));
        }
        return of(new Migration());
    }
}

export const migrationRoute: Routes = [
    {
        path: 'migration',
        component: MigrationComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.migration.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'migration/:id/view',
        component: MigrationDetailComponent,
        resolve: {
            migration: MigrationResolve
        },
        data: {
            authorities: [],
            pageTitle: 'svn2GitApp.migration.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'migration/new',
        component: MigrationUpdateComponent,
        resolve: {
            migration: MigrationResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.migration.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'migration/:id/edit',
        component: MigrationUpdateComponent,
        resolve: {
            migration: MigrationResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.migration.home.title'
        },
        canActivate: [UserRouteAccessService]
    }
];

export const migrationPopupRoute: Routes = [
    {
        path: 'migration/:id/delete',
        component: MigrationDeletePopupComponent,
        resolve: {
            migration: MigrationResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.migration.home.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    }
];
