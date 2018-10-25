/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { HttpHeaders, HttpResponse } from '@angular/common/http';

import { Svn2GitTestModule } from '../../../test.module';
import { MigrationHistoryComponent } from 'app/entities/migration-history/migration-history.component';
import { MigrationHistoryService } from 'app/entities/migration-history/migration-history.service';
import { MigrationHistory } from 'app/shared/model/migration-history.model';

describe('Component Tests', () => {
    describe('MigrationHistory Management Component', () => {
        let comp: MigrationHistoryComponent;
        let fixture: ComponentFixture<MigrationHistoryComponent>;
        let service: MigrationHistoryService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [MigrationHistoryComponent],
                providers: []
            })
                .overrideTemplate(MigrationHistoryComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(MigrationHistoryComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(MigrationHistoryService);
        });

        it('Should call load all on init', () => {
            // GIVEN
            const headers = new HttpHeaders().append('link', 'link;link');
            spyOn(service, 'query').and.returnValue(
                of(
                    new HttpResponse({
                        body: [new MigrationHistory(123)],
                        headers
                    })
                )
            );

            // WHEN
            comp.ngOnInit();

            // THEN
            expect(service.query).toHaveBeenCalled();
            expect(comp.migrationHistories[0]).toEqual(jasmine.objectContaining({ id: 123 }));
        });
    });
});
