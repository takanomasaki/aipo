/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2011 Aimluck,Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aimluck.eip.util;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.rundata.JetspeedRunData;
import org.apache.jetspeed.services.rundata.JetspeedRunDataService;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.services.rundata.RunDataService;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.portal.controls.ALVelocityPortletControl.PortletTab;

/**
 * Aimluck Session のユーティリティクラスです。 <br />
 * 
 */
public class ALSessionUtils {

  /** logger */
  @SuppressWarnings("unused")
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ALSessionUtils.class.getName());

  private static List<String> imageScreenList;

  private static List<String> jsonScreenList;

  static {
    imageScreenList = new ArrayList<String>(5);
    imageScreenList.add("FileuploadFacePhotoScreen");
    imageScreenList.add("TimelineFileThumbnailScreen");
    imageScreenList.add("TimelineUrlThumbnailScreen");
    imageScreenList.add("BlogFileThumbnailScreen");
    imageScreenList.add("MsgboardTopicFileThumbnailScreen");
  };

  public static List<String> getImageScreenList() {
    return imageScreenList;
  }

  public static List<String> getJsonScreenList() {
    if (jsonScreenList == null) {
      jsonScreenList = new ArrayList<String>();
    }
    return jsonScreenList;
  }

  public static boolean isImageRequest(RunData data) {
    String template = data.getParameters().getString("template");
    if (template == null) {
      return false;
    }
    if (getImageScreenList().contains(template)) {
      data.getRequest().setAttribute(
        "com.aimluck.eip.util.ALSessionUtils.isImageRequest",
        true);
      return true;
    }
    return false;
  }

  public static boolean isJsonScreen(RunData data) {
    String template = data.getParameters().getString("template");
    if (template == null) {
      return false;
    }

    if (template.endsWith("JSONScreen")) {
      return true;
    }

    // if (getJsonScreenList().contains(template)) {
    // return true;
    // }
    return false;
  }

  public static RunData getRundata() {
    JetspeedRunDataService runDataService =
      (JetspeedRunDataService) TurbineServices.getInstance().getService(
        RunDataService.SERVICE_NAME);
    if (runDataService != null) {
      JetspeedRunData jrundata = runDataService.getCurrentRunData();
      if (jrundata != null) {
        return jrundata;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public static void refleshMenusSession(RunData data, Context context) {
    HttpSession session = data.getSession();
    List<PortletTab> accountMenus =
      (List<PortletTab>) session.getAttribute("accountMenus");
    List<PortletTab> systemMenus =
      (List<PortletTab>) session.getAttribute("systemMenus");
    context.put("accountMenus", accountMenus);
    context.put("systemMenus", systemMenus);
  }
}
