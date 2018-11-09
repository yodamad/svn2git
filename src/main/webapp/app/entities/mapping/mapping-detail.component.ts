import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IMapping } from 'app/shared/model/mapping.model';

@Component({
    selector: 'jhi-mapping-detail',
    templateUrl: './mapping-detail.component.html'
})
export class MappingDetailComponent implements OnInit {
    mapping: IMapping;

    constructor(private activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ mapping }) => {
            this.mapping = mapping;
        });
    }

    previousState() {
        window.history.back();
    }
}
