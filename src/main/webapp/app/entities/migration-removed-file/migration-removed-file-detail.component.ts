import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IMigrationRemovedFile } from 'app/shared/model/migration-removed-file.model';

@Component({
    selector: 'jhi-migration-removed-file-detail',
    templateUrl: './migration-removed-file-detail.component.html'
})
export class MigrationRemovedFileDetailComponent implements OnInit {
    migrationRemovedFile: IMigrationRemovedFile;

    constructor(protected activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ migrationRemovedFile }) => {
            this.migrationRemovedFile = migrationRemovedFile;
        });
    }

    previousState() {
        window.history.back();
    }
}
