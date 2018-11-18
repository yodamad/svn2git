import { Component, Inject } from '@angular/core';

import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';

@Component({
    selector: 'jhi-add-mapping-modal',
    templateUrl: './add-mapping.component.html'
})
export class JhiAddMappingModalComponent {
    constructor(public dialogRef: MatDialogRef<JhiAddMappingModalComponent>, @Inject(MAT_DIALOG_DATA) public data: any) {}

    cancel() {
        this.dialogRef.close();
    }
}
