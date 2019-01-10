package com.until

import java.sql.Timestamp

/**
  * @Author: king
  * @Date: 2019-01-09
  * @Desc: TODO
  */

object TimeParseUtil {
  def parse(time: String): Timestamp = {
    if (!time.isEmpty) {
      Timestamp.valueOf(time)
    }
    else {
      Timestamp.valueOf("2018-02-24 00:21:55")
    }
  }

}
