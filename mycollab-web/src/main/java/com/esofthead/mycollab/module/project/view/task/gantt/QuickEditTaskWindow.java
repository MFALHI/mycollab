/**
 * This file is part of mycollab-web.
 *
 * mycollab-web is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mycollab-web is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with mycollab-web.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.esofthead.mycollab.module.project.view.task.gantt;

import com.esofthead.mycollab.common.i18n.GenericI18Enum;
import com.esofthead.mycollab.eventmanager.EventBusFactory;
import com.esofthead.mycollab.module.project.domain.SimpleTask;
import com.esofthead.mycollab.module.project.domain.Task;
import com.esofthead.mycollab.module.project.events.TaskEvent;
import com.esofthead.mycollab.module.project.i18n.TaskI18nEnum;
import com.esofthead.mycollab.module.project.service.ProjectTaskService;
import com.esofthead.mycollab.module.project.view.settings.component.ProjectMemberSelectionField;
import com.esofthead.mycollab.spring.ApplicationContextUtil;
import com.esofthead.mycollab.vaadin.AppContext;
import com.esofthead.mycollab.vaadin.ui.*;
import com.esofthead.mycollab.vaadin.ui.grid.GridFormLayoutHelper;
import com.rits.cloning.Cloner;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import org.vaadin.viritin.layouts.MHorizontalLayout;

/**
 * @author MyCollab Ltd
 * @since 5.1.1
 */
public class QuickEditTaskWindow extends Window {
    private GanttExt gantt;
    private GanttItemWrapper taskModified;

    public QuickEditTaskWindow(GanttExt gantt, GanttItemWrapper task) {
        super("Quick Edit Task");
        this.gantt = gantt;
        this.taskModified = task;
        this.setWidth("800px");
        this.setModal(true);
        this.setResizable(false);
        this.setClosable(true);
        this.center();

        EditForm editForm = new EditForm();
        Cloner cloner = new Cloner();
        editForm.setBean(cloner.deepClone(task.getTask()));
        this.setContent(editForm);
    }

    private class EditForm extends AdvancedEditBeanForm<SimpleTask> {
        @Override
        public void setBean(final SimpleTask item) {
            this.setFormLayoutFactory(new FormLayoutFactory());
            this.setBeanFormFieldFactory(new EditFormFieldFactory(EditForm.this));
            super.setBean(item);
        }

        class FormLayoutFactory implements IFormLayoutFactory {
            private static final long serialVersionUID = 1L;
            private GridFormLayoutHelper informationLayout;

            @Override
            public ComponentContainer getLayout() {
                VerticalLayout layout = new VerticalLayout();
                informationLayout = GridFormLayoutHelper.defaultFormLayoutHelper(2, 3);
                informationLayout.getLayout().setMargin(false);
                informationLayout.getLayout().setSpacing(false);
                layout.addComponent(informationLayout.getLayout());

                MHorizontalLayout buttonControls = new MHorizontalLayout().withMargin(new MarginInfo(true, true, true, false));
                buttonControls.setDefaultComponentAlignment(Alignment.MIDDLE_RIGHT);

                Button updateAllBtn = new Button("Update other fields", new Button.ClickListener() {
                    @Override
                    public void buttonClick(Button.ClickEvent clickEvent) {
                        EventBusFactory.getInstance().post(new TaskEvent.GotoEdit(QuickEditTaskWindow.this, EditForm.this.bean));
                        close();
                    }
                });
                updateAllBtn.addStyleName(UIConstants.THEME_LINK);

                Button updateBtn = new Button(AppContext.getMessage(GenericI18Enum.BUTTON_UPDATE_LABEL), new Button.ClickListener() {
                    @Override
                    public void buttonClick(Button.ClickEvent clickEvent) {
                        if (EditForm.this.validateForm()) {
                            ProjectTaskService taskService = ApplicationContextUtil.getSpringBean(ProjectTaskService.class);
                            taskService.updateWithSession(bean, AppContext.getUsername());
                            SimpleTask updateTask = taskService.findById(bean.getId(), AppContext.getAccountId());
                            taskModified.setTask(updateTask);
                            taskModified.markAsDirty();
                            gantt.markStepDirty(taskModified.getStep());
                            gantt.calculateMaxMinDates(taskModified);
                            EventBusFactory.getInstance().post(new TaskEvent.GanttTaskUpdate(QuickEditTaskWindow.this, taskModified));
                            close();
                        }
                    }
                });
                updateBtn.setStyleName(UIConstants.THEME_GREEN_LINK);

                Button cancelBtn = new Button(AppContext.getMessage(GenericI18Enum.BUTTON_CANCEL), new Button.ClickListener() {
                    @Override
                    public void buttonClick(Button.ClickEvent clickEvent) {
                        QuickEditTaskWindow.this.close();
                    }
                });
                cancelBtn.setStyleName(UIConstants.THEME_GRAY_LINK);
                buttonControls.with(updateAllBtn, updateBtn, cancelBtn);

                layout.addComponent(buttonControls);
                layout.setComponentAlignment(buttonControls, Alignment.MIDDLE_RIGHT);
                return layout;
            }

            @Override
            public void attachField(Object propertyId, Field<?> field) {
                if (Task.Field.taskname.equalTo(propertyId)) {
                    informationLayout.addComponent(field, AppContext.getMessage(TaskI18nEnum.FORM_TASK_NAME), 0, 0, 2, "100%");
                } else if (Task.Field.startdate.equalTo(propertyId)) {
                    informationLayout.addComponent(field, AppContext.getMessage(TaskI18nEnum.FORM_START_DATE), 0, 1);
                } else if (Task.Field.enddate.equalTo(propertyId)) {
                    informationLayout.addComponent(field, AppContext.getMessage(TaskI18nEnum.FORM_END_DATE), 1, 1);
                } else if (Task.Field.deadline.equalTo(propertyId)) {
                    informationLayout.addComponent(field, AppContext.getMessage(TaskI18nEnum.FORM_DEADLINE), 0, 2);
                } else if (Task.Field.assignuser.equalTo(propertyId)) {
                    informationLayout.addComponent(field, AppContext.getMessage(GenericI18Enum.FORM_ASSIGNEE), 1, 2);
                }
            }
        }

        private class EditFormFieldFactory extends AbstractBeanFieldGroupEditFieldFactory<SimpleTask> {
            private static final long serialVersionUID = 1L;

            public EditFormFieldFactory(GenericBeanForm<SimpleTask> form) {
                super(form);
            }

            @Override
            protected Field<?> onCreateField(final Object propertyId) {
                if (Task.Field.assignuser.equalTo(propertyId)) {
                    return new ProjectMemberSelectionField();
                }
                return null;
            }
        }
    }
}
