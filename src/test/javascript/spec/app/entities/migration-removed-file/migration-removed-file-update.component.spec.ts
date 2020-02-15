/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { Svn2GitTestModule } from '../../../test.module';
import { MigrationRemovedFileUpdateComponent } from 'app/entities/migration-removed-file/migration-removed-file-update.component';
import { MigrationRemovedFileService } from 'app/entities/migration-removed-file/migration-removed-file.service';
import { MigrationRemovedFile } from 'app/shared/model/migration-removed-file.model';

describe('Component Tests', () => {
    describe('MigrationRemovedFile Management Update Component', () => {
        let comp: MigrationRemovedFileUpdateComponent;
        let fixture: ComponentFixture<MigrationRemovedFileUpdateComponent>;
        let service: MigrationRemovedFileService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [Svn2GitTestModule],
                declarations: [MigrationRemovedFileUpdateComponent]
            })
                .overrideTemplate(MigrationRemovedFileUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(MigrationRemovedFileUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(MigrationRemovedFileService);
        });

        describe('save', () => {
            it('Should call update service on save for existing entity', fakeAsync(() => {
                // GIVEN
                const entity = new MigrationRemovedFile(123);
                spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.migrationRemovedFile = entity;
                // WHEN
                comp.save();
                tick(); // simulate async

                // THEN
                expect(service.update).toHaveBeenCalledWith(entity);
                expect(comp.isSaving).toEqual(false);
            }));

            it('Should call create service on save for new entity', fakeAsync(() => {
                // GIVEN
                const entity = new MigrationRemovedFile();
                spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.migrationRemovedFile = entity;
                // WHEN
                comp.save();
                tick(); // simulate async

                // THEN
                expect(service.create).toHaveBeenCalledWith(entity);
                expect(comp.isSaving).toEqual(false);
            }));
        });
    });
});
