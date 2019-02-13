import { Component, Inject } from '@angular/core';

import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';
import { MigrationService } from 'app/entities/migration';

@Component({
    selector: 'jhi-confirm-retry-modal',
    templateUrl: './confirm-retry.component.html'
})
export class JhiConfirmRetryModalComponent {
    constructor(
        public dialogRef: MatDialogRef<JhiConfirmRetryModalComponent>,
        private _migrationService: MigrationService,
        @Inject(MAT_DIALOG_DATA) public data: any
    ) {}

    retry() {
        this._migrationService.retry(this.data.migId).subscribe(res => this.dialogRef.close());
    }

    cancel() {
        this.dialogRef.close();
    }
}
