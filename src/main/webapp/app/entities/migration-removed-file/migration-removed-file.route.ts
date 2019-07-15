import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core';
import { of } from 'rxjs';
import { map } from 'rxjs/operators';
import { MigrationRemovedFile } from 'app/shared/model/migration-removed-file.model';
import { MigrationRemovedFileService } from './migration-removed-file.service';
import { MigrationRemovedFileComponent } from './migration-removed-file.component';
import { MigrationRemovedFileDetailComponent } from './migration-removed-file-detail.component';
import { MigrationRemovedFileUpdateComponent } from './migration-removed-file-update.component';
import { MigrationRemovedFileDeletePopupComponent } from './migration-removed-file-delete-dialog.component';
import { IMigrationRemovedFile } from 'app/shared/model/migration-removed-file.model';

@Injectable({ providedIn: 'root' })
export class MigrationRemovedFileResolve implements Resolve<IMigrationRemovedFile> {
    constructor(private service: MigrationRemovedFileService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(map((migrationRemovedFile: HttpResponse<MigrationRemovedFile>) => migrationRemovedFile.body));
        }
        return of(new MigrationRemovedFile());
    }
}

export const migrationRemovedFileRoute: Routes = [
    {
        path: 'migration-removed-file',
        component: MigrationRemovedFileComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.migrationRemovedFile.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'migration-removed-file/:id/view',
        component: MigrationRemovedFileDetailComponent,
        resolve: {
            migrationRemovedFile: MigrationRemovedFileResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.migrationRemovedFile.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'migration-removed-file/new',
        component: MigrationRemovedFileUpdateComponent,
        resolve: {
            migrationRemovedFile: MigrationRemovedFileResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.migrationRemovedFile.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'migration-removed-file/:id/edit',
        component: MigrationRemovedFileUpdateComponent,
        resolve: {
            migrationRemovedFile: MigrationRemovedFileResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.migrationRemovedFile.home.title'
        },
        canActivate: [UserRouteAccessService]
    }
];

export const migrationRemovedFilePopupRoute: Routes = [
    {
        path: 'migration-removed-file/:id/delete',
        component: MigrationRemovedFileDeletePopupComponent,
        resolve: {
            migrationRemovedFile: MigrationRemovedFileResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'svn2GitApp.migrationRemovedFile.home.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    }
];
