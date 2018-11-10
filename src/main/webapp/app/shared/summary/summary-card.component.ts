import { Component, Input, OnInit } from '@angular/core';
import { IMigration } from 'app/shared/model/migration.model';
import { ConfigurationService } from 'app/shared/service/configuration-service';

@Component({
    selector: 'jhi-summary-card',
    templateUrl: 'summary-card.component.html',
    styleUrls: ['summary-card.component.css']
})
export class SummaryCardComponent {
    @Input() migration: IMigration;
}
