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

package com.aimluck.eip.modules.actions.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jetspeed.modules.actions.portlets.SecureVelocityPortletAction;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.services.orgutils.ALOrgUtilsService;
import com.aimluck.eip.util.ALCommonUtils;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALSessionUtils;

/**
 * Velocity Portlet を扱う際の抽象クラスです。 <br />
 * 
 */
public abstract class ALSecureBaseAction extends SecureVelocityPortletAction
    implements ALAction {

  /** logger */
  @SuppressWarnings("unused")
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ALSecureBaseAction.class.getName());

  /** 表示モード */
  private String mode;

  /** result */
  private Object result;

  /** 検索結果を格納するリスト */
  private List<Object> resultList;

  /** 異常系のメッセージを格納するリスト */
  private List<String> errmsgList;

  /**
   * 
   * @param obj
   */
  @Override
  public void setResultData(Object obj) {
    result = obj;
  }

  /**
   * 
   * @param obj
   */
  @Override
  public void addResultData(Object obj) {
    if (resultList == null) {
      resultList = new ArrayList<Object>();
    }
    resultList.add(obj);
  }

  /**
   * 
   * @param objList
   */
  @Override
  public void setResultDataList(List<Object> objList) {
    resultList = objList;
  }

  /**
   * 
   * @param msg
   */
  @Override
  public void addErrorMessages(List<String> msgs) {
    if (errmsgList == null) {
      errmsgList = new ArrayList<String>();
    }
    errmsgList.addAll(msgs);
  }

  /**
   * 
   * @param msg
   */
  @Override
  public void addErrorMessage(String msg) {
    if (errmsgList == null) {
      errmsgList = new ArrayList<String>();
    }
    errmsgList.add(msg);
  }

  /**
   * 
   * @param msgs
   */
  @Override
  public void setErrorMessages(List<String> msgs) {
    errmsgList = msgs;
  }

  /**
   * 
   * @param mode
   */
  @Override
  public void setMode(String mode) {
    this.mode = mode;
  }

  /**
   * 
   * @return
   */
  @Override
  public String getMode() {
    return mode;
  }

  /**
   * 
   * @param context
   */
  @Override
  public void putData(RunData rundata, Context context) {
    context.put(ALEipConstants.MODE, mode);
    context.put(ALEipConstants.RESULT, result);
    context.put(ALEipConstants.ERROR_MESSAGE_LIST, errmsgList);
    context.put(ALEipConstants.ENTITY_ID, ALEipUtils.getTemp(
      rundata,
      context,
      ALEipConstants.ENTITY_ID));
    context.put("utils", new ALCommonUtils());
    ALSessionUtils.refleshMenusSession(rundata, context);
    Map<String, String> attribute = ALOrgUtilsService.getParameters();
    for (Map.Entry<String, String> e : attribute.entrySet()) {
      context.put(e.getKey(), e.getValue());
    }
    if (Boolean.parseBoolean((String) rundata.getSession().getAttribute(
      "changeToPc"))) { // PC表示切り替え用
      context.put("client", ALEipUtils.getClient(rundata));
    }

    // For security
    context.put(ALEipConstants.SECURE_ID, rundata.getUser().getTemp(
      ALEipConstants.SECURE_ID));
  }

}
