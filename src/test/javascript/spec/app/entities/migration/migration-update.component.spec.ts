/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { Svn2GitTestModule } from '../../../test.module';
import { MigrationUpdateComponent } from 'app/entities/migration/migration-update.component';
import { MigrationService } from 'app/entities/migration/migration.service';
import { Migration } from 'app/shared/model/migration.model';

describe('Component Tests', () => {
    describe('Migration Management Update Component', () => {
        let comp: MigrationUpdateComponent;
        let fixture: ComponentFixture<MigrationUpdateComponent>;
        let service: MigrationService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [MigrationUpdateComponent]
            })
                .overrideTemplate(MigrationUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(MigrationUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(MigrationService);
        });

        describe('save', () => {
            it(
                'Should call update service on save for existing entity',
                fakeAsync(() => {
                    // GIVEN
                    const entity = new Migration(123);
                    spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                    comp.migration = entity;
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
                    const entity = new Migration();
                    spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                    comp.migration = entity;
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
