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
package com.esofthead.mycollab.module.crm.ui.components;

import com.esofthead.mycollab.common.domain.SimpleComment;
import com.esofthead.mycollab.common.i18n.GenericI18Enum;
import com.esofthead.mycollab.common.service.CommentService;
import com.esofthead.mycollab.module.ecm.domain.Content;
import com.esofthead.mycollab.module.user.ui.components.UserBlock;
import com.esofthead.mycollab.spring.ApplicationContextUtil;
import com.esofthead.mycollab.vaadin.AppContext;
import com.esofthead.mycollab.vaadin.ui.*;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.apache.commons.collections.CollectionUtils;
import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;

import java.util.List;

/**
 * @author MyCollab Ltd.
 * @since 5.0.4
 */
public class CommentRowDisplayHandler extends BeanList.RowDisplayHandler<SimpleComment> {
    private static final long serialVersionUID = 1L;

    @Override
    public Component generateRow(final SimpleComment comment, int rowIndex) {
        final MHorizontalLayout layout = new MHorizontalLayout().withMargin(new MarginInfo(true, true, true, false))
                .withWidth("100%").withStyleName("message");

        UserBlock memberBlock = new UserBlock(comment.getCreateduser(), comment.getOwnerAvatarId(), comment.getOwnerFullName());
        layout.addComponent(memberBlock);

        CssLayout rowLayout = new CssLayout();
        rowLayout.setStyleName("message-container");
        rowLayout.setWidth("100%");

        MHorizontalLayout messageHeader = new MHorizontalLayout().withMargin(new MarginInfo(true,
                true, false, true)).withWidth("100%").withStyleName("message-header");
        messageHeader.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);

        ELabel timePostLbl = new ELabel(AppContext.getMessage(GenericI18Enum.EXT_ADDED_COMMENT, comment.getOwnerFullName(),
                AppContext.formatPrettyTime(comment.getCreatedtime())), ContentMode.HTML).withDescription(AppContext.
                formatDateTime(comment.getCreatedtime()));

        timePostLbl.setSizeUndefined();
        timePostLbl.setStyleName("time-post");
        messageHeader.with(timePostLbl).expand(timePostLbl);

        // Message delete button
        Button msgDeleteBtn = new Button();
        msgDeleteBtn.setIcon(FontAwesome.TRASH_O);
        msgDeleteBtn.setStyleName(UIConstants.BUTTON_ICON_ONLY);
        messageHeader.addComponent(msgDeleteBtn);

        if (hasDeletePermission(comment)) {
            msgDeleteBtn.setVisible(true);
            msgDeleteBtn.addClickListener(new Button.ClickListener() {
                private static final long serialVersionUID = 1L;

                @Override
                public void buttonClick(Button.ClickEvent event) {
                    ConfirmDialogExt.show(UI.getCurrent(),
                            AppContext.getMessage(GenericI18Enum.DIALOG_DELETE_TITLE, AppContext.getSiteName()),
                            AppContext.getMessage(GenericI18Enum.DIALOG_DELETE_SINGLE_ITEM_MESSAGE),
                            AppContext.getMessage(GenericI18Enum.BUTTON_YES),
                            AppContext.getMessage(GenericI18Enum.BUTTON_NO),
                            new ConfirmDialog.Listener() {
                                private static final long serialVersionUID = 1L;

                                @Override
                                public void onClose(ConfirmDialog dialog) {
                                    if (dialog.isConfirmed()) {
                                        CommentService commentService = ApplicationContextUtil.getSpringBean(CommentService.class);
                                        commentService.removeWithSession(comment, AppContext.getUsername(), AppContext.getAccountId());
                                        owner.removeRow(layout);
                                    }
                                }
                            });
                }
            });
        } else {
            msgDeleteBtn.setVisible(false);
        }

        rowLayout.addComponent(messageHeader);

        Label messageContent = new SafeHtmlLabel(comment.getComment());
        messageContent.setStyleName("message-body");
        rowLayout.addComponent(messageContent);

        List<Content> attachments = comment.getAttachments();
        if (!CollectionUtils.isEmpty(attachments)) {
            MVerticalLayout messageFooter = new MVerticalLayout().withSpacing(false).withWidth
                    ("100%").withStyleName("message-footer");
            AttachmentDisplayComponent attachmentDisplay = new AttachmentDisplayComponent(
                    attachments);
            attachmentDisplay.setWidth("100%");
            messageFooter.with(attachmentDisplay).withAlign(attachmentDisplay, Alignment.MIDDLE_RIGHT);
            rowLayout.addComponent(messageFooter);
        }

        layout.with(rowLayout).expand(rowLayout);
        return layout;
    }

    private boolean hasDeletePermission(SimpleComment comment) {
        return (AppContext.getUsername().equals(comment.getCreateduser()) || AppContext.isAdmin());
    }
}
