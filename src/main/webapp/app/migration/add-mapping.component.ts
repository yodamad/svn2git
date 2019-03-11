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

    isNOK(): boolean {
        const undef =
            this.data.staticMapping.gitDirectory === undefined ||
            this.data.staticMapping.svnDirectory === undefined ||
            this.data.staticMapping.regex === undefined;
        const empty =
            this.data.staticMapping.gitDirectory === '' ||
            this.data.staticMapping.svnDirectory === '' ||
            this.data.staticMapping.regex === '';

        return undef || empty;
    }
}
