import { Component, Inject } from '@angular/core';

import { MAT_DIALOG_DATA, MatDialogRef, MatRadioChange } from '@angular/material';

@Component({
    selector: 'jhi-add-mapping-modal',
    templateUrl: './add-mapping.component.html'
})
export class JhiAddMappingModalComponent {
    radioChosen = 0;

    radioChoices: string[] = ['Apply Mapping', 'Not Migrated from SVN'];

    constructor(public dialogRef: MatDialogRef<JhiAddMappingModalComponent>, @Inject(MAT_DIALOG_DATA) public data: any) {}

    cancel() {
        this.dialogRef.close();
    }

    isNOK(): boolean {
        const undef =
            this.data.staticMapping.gitDirectory === undefined ||
            this.data.staticMapping.svnDirectory === undefined ||
            this.data.staticMapping.regex === undefined ||
            this.data.staticMapping.svnDirectoryDelete === undefined;
        const empty =
            this.data.staticMapping.gitDirectory === '' ||
            this.data.staticMapping.svnDirectory === '' ||
            this.data.staticMapping.regex === '' ||
            this.data.staticMapping.svnDirectoryDelete === '';

        const undef2 = this.data.staticMapping.svnDirectory === undefined || this.data.staticMapping.svnDirectoryDelete === undefined;
        const empty2 = this.data.staticMapping.svnDirectory === '' || this.data.staticMapping.svnDirectoryDelete === '';

        if (this.radioChosen === 0) {
            return undef || empty;
        } else {
            return undef2 || empty2;
        }
    }

    radioChange($event: MatRadioChange) {
        console.log($event.value);

        if ($event.value === 0) {
            this.data.staticMapping.svnDirectoryDelete = false;
        } else {
            this.data.staticMapping.svnDirectoryDelete = true;
            this.data.staticMapping.regex = '';
            this.data.staticMapping.gitDirectory = '';
        }
    }

    isSvnDirectoryDeleteSelected() {
        return this.radioChosen === 1;
    }
}
