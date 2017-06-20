/*
 * Copyright (c)  2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.extension.siddhi.execution.time;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.log4j.Logger;
import org.wso2.extension.siddhi.execution.time.util.TimeExtensionConstants;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.annotation.Parameter;
import org.wso2.siddhi.annotation.ReturnAttribute;
import org.wso2.siddhi.annotation.util.DataType;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.exception.SiddhiAppRuntimeException;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.executor.function.FunctionExecutor;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.exception.SiddhiAppValidationException;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * dateDiff(dateValue1,dateValue2,dateFormat1,dateFormat2)/dateDiff(dateValue1,dateValue2)/
 * dateDiff(timestampInMilliseconds1,timestampInMilliseconds2)
 * Returns time(days) between two dates.
 * dateValue1 - value of date. eg: "2014-11-11 13:23:44.657", "2014-11-11" , "13:23:44.657"
 * dateValue2 - value of date. eg: "2014-11-11 13:23:44.657", "2014-11-11" , "13:23:44.657"
 * dateFormat1 - Date format of the provided dateValue1. eg: yyyy-MM-dd HH:mm:ss.SSS
 * dateFormat2 - Date format of the provided dateValue2. eg: yyyy-MM-dd HH:mm:ss.SSS
 * timestampInMilliseconds1 - date value in milliseconds.(from the epoch) eg: 1415712224000L
 * timestampInMilliseconds2 - date value in milliseconds.(from the epoch) eg: 1423456224000L
 * Accept Type(s) for dateDiff(dateValue1,dateValue2,dateFormat1,dateFormat2):
 *         dateValue1 : STRING
 *         dateValue2 : STRING
 *         dateFormat1 : STRING
 *         dateFormat2 : STRING
 * Accept Type(s) for dateDiff(timestampInMilliseconds1,timestampInMilliseconds2):
 *         timestampInMilliseconds1 : LONG
 *         timestampInMilliseconds2 : LONG
 * Return Type(s): INT
 */

/**
 * Class representing the Time dateDiff implementation.
 */
@Extension(
        name = "dateDiff",
        namespace = "time",
        description = "Returns time(days) between two dates. If two STRING arguments are sent as first two parameters" +
                      " then function should accept four parameters with last two as optional parameters. Parameter " +
                      "order should be dateDiff(date.value1,date.value2,date.format1,date.format2). Else if two LONG " +
                      "arguments are sent as first two parameters then parameter order should be dateDiff(timestamp" +
                      ".in.milliseconds1,timestamp.in.milliseconds2). ",
        parameters = {
                @Parameter(name = "date.value1",
                        description = "value of date. eg: \"2014-11-11 13:23:44.657\", \"2014-11-11\" , " +
                                      "\"13:23:44.657\".",
                        type = {DataType.STRING}),
                @Parameter(name = "date.value2",
                        description = "value of date. eg: \"2014-11-11 13:23:44.657\", \"2014-11-11\" , " +
                                      "\"13:23:44.657\".",
                        type = {DataType.STRING}),
                @Parameter(name = "date.format1",
                        description = "Date format of the provided date.value1. eg: yyyy-MM-dd HH:mm:ss.SSS.",
                        type = {DataType.STRING},
                        optional = true,
                        defaultValue = "yyyy-MM-dd HH:mm:ss.SSS"),
                @Parameter(name = "date.format2",
                        description = "Date format of the provided date.value2. eg: yyyy-MM-dd HH:mm:ss.SSS.",
                        type = {DataType.STRING},
                        optional = true,
                        defaultValue = "yyyy-MM-dd HH:mm:ss.SSS"),
                @Parameter(name = "timestamp.in.milliseconds1",
                        description = "date value in milliseconds.(from the epoch) eg: 1415712224000L.",
                        type = {DataType.LONG}),
                @Parameter(name = "timestamp.in.milliseconds2",
                        description = "date value in milliseconds.(from the epoch) eg: 1415712224000L.",
                        type = {DataType.LONG})
        },
        returnAttributes = @ReturnAttribute(
                description = "Returned type will be int.",
                type = {DataType.INT}),
        examples = {
                @Example(
                        syntax = "TBD",
                        description = "TBD"
                )
        }
)
public class DateDifferenceFunctionExtension extends FunctionExecutor {

    private Attribute.Type returnType = Attribute.Type.LONG;
    private static final Logger log = Logger.getLogger(DateDifferenceFunctionExtension.class);
    private boolean useDefaultDateFormat = false;
    private String firstDateFormat = null;
    private String secondDateFormat = null;
    private Calendar firstCalInstance = Calendar.getInstance();
    private Calendar secondCalInstance = Calendar.getInstance();

    @Override
    protected void init(ExpressionExecutor[] expressionExecutors, ConfigReader configReader,
                        SiddhiAppContext siddhiAppContext) {

        if (attributeExpressionExecutors[0].getReturnType() != Attribute.Type.LONG && attributeExpressionExecutors
                .length == 3 || attributeExpressionExecutors[0].getReturnType() != Attribute.Type.LONG &&
                attributeExpressionExecutors.length == 2) {
            useDefaultDateFormat = true;
            firstDateFormat = TimeExtensionConstants.EXTENSION_TIME_DEFAULT_DATE_FORMAT;
            secondDateFormat = TimeExtensionConstants.EXTENSION_TIME_DEFAULT_DATE_FORMAT;
        }
        if (attributeExpressionExecutors.length == 4) {
            if (attributeExpressionExecutors[0].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the first argument of " +
                                                       "time:dateDiff(dateValue1,dateValue2,dateFormat1,dateFormat2) " +
                                                       "function, " + "required " + Attribute.Type.STRING +
                                                       " but found " + attributeExpressionExecutors[0]
                                                               .getReturnType().toString());
            }
            if (attributeExpressionExecutors[1].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the second argument of " +
                        "time:dateDiff(dateValue1,dateValue2,dateFormat1,dateFormat2) function, " + "required "
                        + Attribute.Type.STRING +
                        " but found " + attributeExpressionExecutors[1].getReturnType().toString());
            }
            if (attributeExpressionExecutors[2].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the third argument of " +
                        "time:dateDiff(dateValue1,dateValue2,dateFormat1,dateFormat2) function, " + "required "
                        + Attribute.Type.STRING +
                        " but found " + attributeExpressionExecutors[2].getReturnType().toString());
            }
            if (attributeExpressionExecutors[3].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the fourth argument of " +
                        "time:dateDiff(dateValue1,dateValue2,dateFormat1,dateFormat2) function, " + "required "
                        + Attribute.Type.STRING +
                        " but found " + attributeExpressionExecutors[3].getReturnType().toString());
            }
        } else if (attributeExpressionExecutors.length == 2) {
            if (useDefaultDateFormat) {
                if (attributeExpressionExecutors[0].getReturnType() != Attribute.Type.STRING) {
                    throw new SiddhiAppValidationException("Invalid parameter type found for " +
                                                               "the first argument of " +
                            "time:dateDiff(dateValue1,dateValue2,dateFormat1,dateFormat2) function, " + "required "
                            + Attribute.Type.STRING +
                            " but found " + attributeExpressionExecutors[0].getReturnType().toString());
                }
                if (attributeExpressionExecutors[1].getReturnType() != Attribute.Type.STRING) {
                    throw new SiddhiAppValidationException("Invalid parameter type found for " +
                                                               "the second argument of " +
                            "time:dateDiff(dateValue1,dateValue2,dateFormat1,dateFormat2) function, " + "required "
                            + Attribute.Type.STRING +
                            " but found " + attributeExpressionExecutors[1].getReturnType().toString());
                }
            } else {
                if (attributeExpressionExecutors[0].getReturnType() != Attribute.Type.LONG) {
                    throw new SiddhiAppValidationException("Invalid parameter type found for " +
                                                               "the first argument of " +
                            "time:dateDiff(timestampInMilliseconds1,timestampInMilliseconds2) function, " +
                            "" + "required " + Attribute.Type.LONG +
                            " but found " + attributeExpressionExecutors[0].getReturnType().toString());
                }
                if (attributeExpressionExecutors[1].getReturnType() != Attribute.Type.LONG) {
                    throw new SiddhiAppValidationException("Invalid parameter type found for " +
                                                               "the second argument of " +
                            "time:dateDiff(timestampInMilliseconds1,timestampInMilliseconds2) function, " +
                            "" + "required " + Attribute.Type.LONG +
                            " but found " + attributeExpressionExecutors[1].getReturnType().toString());
                }
            }

        } else if (attributeExpressionExecutors.length == 3 && useDefaultDateFormat) {
            if (attributeExpressionExecutors[0].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for " +
                                                           "the first argument of " +
                        "time:dateDiff(dateValue1,dateValue2,dateFormat1,dateFormat2) function, " + "required "
                        + Attribute.Type.STRING +
                        " but found " + attributeExpressionExecutors[0].getReturnType().toString());
            }
            if (attributeExpressionExecutors[1].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the second argument of " +
                        "time:dateDiff(dateValue1,dateValue2,dateFormat1,dateFormat2) function, " + "required "
                        + Attribute.Type.STRING +
                        " but found " + attributeExpressionExecutors[1].getReturnType().toString());
            }
            if (attributeExpressionExecutors[2].getReturnType() != Attribute.Type.STRING) {
                throw new SiddhiAppValidationException("Invalid parameter type found for the third argument of " +
                        "time:dateDiff(dateValue1,dateValue2,dateFormat1,dateFormat2) function, " + "required "
                        + Attribute.Type.STRING +
                        " but found " + attributeExpressionExecutors[2].getReturnType().toString());
            }
        } else {
            throw new SiddhiAppValidationException("Invalid no of arguments passed to time:dateDiff() function, " +
                    "required 2 or 4, but found " + attributeExpressionExecutors.length);
        }

    }

    @Override
    protected Object execute(Object[] data) {

        String firstDate = null;
        String secondDate;
        FastDateFormat userSpecifiedFirstFormat;
        FastDateFormat userSpecifiedSecondFormat;

        if (data.length == 4 || useDefaultDateFormat) {
            try {
                if (data[0] == null) {
                    throw new SiddhiAppRuntimeException("Invalid input given to time:dateDiff(dateValue1," +
                                                        "dateValue2,dateFormat1,dateFormat2) function" + ". " +
                                                        "First " + "argument cannot be null");
                }
                if (data[1] == null) {
                    throw new SiddhiAppRuntimeException("Invalid input given to time:dateDiff(dateValue1," +
                            "dateValue2,dateFormat1,dateFormat2) function" + ". Second " + "argument cannot be null");
                }

                if (!useDefaultDateFormat) {
                    if (data[2] == null) {
                        throw new SiddhiAppRuntimeException("Invalid input given to time:dateDiff(dateValue1," +
                                "dateValue2,dateFormat1,dateFormat2) function" + ". " +
                                                                "Third " + "argument cannot be null");
                    }
                    if (data[3] == null) {
                        throw new SiddhiAppRuntimeException("Invalid input given to time:dateDiff(dateValue1," +
                                "dateValue2,dateFormat1,dateFormat2) function" + ". " +
                                                                "Fourth " + "argument cannot be null");
                    }
                    firstDateFormat = (String) data[2];
                    secondDateFormat = (String) data[3];
                } else {
                    if (data.length != 2) {
                        firstDateFormat = (String) data[2];
                    }
                }
                firstDate = (String) data[0];
                secondDate = (String) data[1];
                userSpecifiedFirstFormat = FastDateFormat.getInstance(firstDateFormat);
                userSpecifiedSecondFormat = FastDateFormat.getInstance(secondDateFormat);
                Date userSpecifiedFirstDate = userSpecifiedFirstFormat.parse(firstDate);
                firstCalInstance.setTime(userSpecifiedFirstDate);
            } catch (ParseException e) {
                String errorMsg =
                        "Provided format " + firstDateFormat + " does not match with the timestamp " + firstDate + e
                                .getMessage();
                throw new SiddhiAppRuntimeException(errorMsg, e);
            } catch (ClassCastException e) {
                String errorMsg = "Provided Data type cannot be cast to desired format. " + e.getMessage();
                throw new SiddhiAppRuntimeException(errorMsg, e);
            }

            try {
                Date userSpecifiedSecondDate = userSpecifiedSecondFormat.parse(secondDate);
                secondCalInstance.setTime(userSpecifiedSecondDate);
            } catch (ParseException e) {
                String errorMsg =
                        "Provided format " + secondDateFormat + " does not match with the timestamp " + secondDate + e
                                .getMessage();
                throw new SiddhiAppRuntimeException(errorMsg, e);
            }

        } else if (data.length == 2) {

            if (data[0] == null) {
                throw new SiddhiAppRuntimeException("Invalid input given to time:dateDiff" +
                        "(timestampInMilliseconds1,timestampInMilliseconds2) function" + ". First " +
                                                        "argument cannot be null");
            }
            if (data[1] == null) {
                throw new SiddhiAppRuntimeException("Invalid input given to time:dateDiff" +
                        "(timestampInMilliseconds1,timestampInMilliseconds2) function" + ". Second " +
                                                        "argument cannot be null");
            }

            try {
                long firstDateInMills = (Long) data[0];
                long secondDateInMills = (Long) data[1];
                firstCalInstance.setTimeInMillis(firstDateInMills);
                secondCalInstance.setTimeInMillis(secondDateInMills);
            } catch (ClassCastException e) {
                String errorMsg = "Provided Data type cannot be cast to desired format. " + e.getMessage();
                throw new SiddhiAppRuntimeException(errorMsg, e);
            }
        } else {
            throw new SiddhiAppRuntimeException("Invalid set of arguments given to time:dateDiff() function." +
                    "Arguments should be either 2 or 4. ");
        }

        long dateDifference = firstCalInstance.getTimeInMillis() - secondCalInstance.getTimeInMillis();
        return TimeUnit.DAYS.convert(dateDifference, TimeUnit.MILLISECONDS);
    }

    @Override
    protected Object execute(Object data) {
        return null; //Since the EpochToDateFormat function takes in 2 parameters, this method does not get called.
        // Hence, not implemented.

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public Attribute.Type getReturnType() {
        return returnType;
    }

    @Override
    public Map<String, Object> currentState() { //No need to maintain a state.
        return null;
    }

    @Override
    public void restoreState(Map<String, Object> state) {
        //Since there's no need to maintain a state, nothing needs to be done here.
    }
}
