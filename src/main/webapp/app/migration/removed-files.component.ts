import { Component, Inject, OnInit } from '@angular/core';

import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';
import { MigrationRemovedFile } from 'app/shared/model/migration-removed-file.model';
import { MigrationService } from 'app/entities/migration';

/**
 * Dialog to display listing of removed files for a migration
 */
@Component({
    selector: 'jhi-removed-files-modal',
    templateUrl: './removed-files.component.html'
})
export class JhiRemovedFilesModalComponent implements OnInit {
    displayedColumns: string[] = ['filePath', 'reason'];
    removedFiles: MigrationRemovedFile[] = [];

    constructor(
        private _migrationService: MigrationService,
        public dialogRef: MatDialogRef<JhiRemovedFilesModalComponent>,
        @Inject(MAT_DIALOG_DATA) public data: any
    ) {}

    ngOnInit(): void {
        this._migrationService.getRemovedFiles(this.data.migrationId).subscribe(res => (this.removedFiles = res.body));
    }

    cancel() {
        this.dialogRef.close();
    }
}
