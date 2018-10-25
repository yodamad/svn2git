/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { Svn2GitTestModule } from '../../../test.module';
import { MigrationHistoryUpdateComponent } from 'app/entities/migration-history/migration-history-update.component';
import { MigrationHistoryService } from 'app/entities/migration-history/migration-history.service';
import { MigrationHistory } from 'app/shared/model/migration-history.model';

describe('Component Tests', () => {
    describe('MigrationHistory Management Update Component', () => {
        let comp: MigrationHistoryUpdateComponent;
        let fixture: ComponentFixture<MigrationHistoryUpdateComponent>;
        let service: MigrationHistoryService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [MigrationHistoryUpdateComponent]
            })
                .overrideTemplate(MigrationHistoryUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(MigrationHistoryUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(MigrationHistoryService);
        });

        describe('save', () => {
            it(
                'Should call update service on save for existing entity',
                fakeAsync(() => {
                    // GIVEN
                    const entity = new MigrationHistory(123);
                    spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                    comp.migrationHistory = entity;
                    // WHEN
                    comp.save();
                    tick(); // simulate async

                    // THEN
                    expect(service.update).toHaveBeenCalledWith(entity);
                    expect(comp.isSaving).toEqual(false);
                })
            );

            it(
                'Should call create service on save for new entity',
                fakeAsync(() => {
                    // GIVEN
                    const entity = new MigrationHistory();
                    spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                    comp.migrationHistory = entity;
                    // WHEN
                    comp.save();
                    tick(); // simulate async

                    // THEN
                    expect(service.create).toHaveBeenCalledWith(entity);
                    expect(comp.isSaving).toEqual(false);
                })
            );
        });
    });
});
