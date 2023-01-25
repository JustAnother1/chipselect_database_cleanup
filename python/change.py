#!/usr/bin/python3
# -*- coding: utf-8 -*-
import pymysql
import sys

mariadb_connection = None
cursor = None
db_host = None
db_user = None
db_password = None

def connect2localDB():
    global cursor
    global mariadb_connection
    global db_host
    global db_user
    global db_password
    mariadb_connection = pymysql.connect(host=db_host, port=3306, user=db_user, password=db_password, database='microcontrollis', charset='utf8mb4')
    mariadb_connection.encoding = 'utf_8'
    # pymysql.set_character_set('utf8')
    #mariadb_connection.set_character_set('utf8')
    cursor = mariadb_connection.cursor()
    cursor.execute('SET NAMES utf8mb4;')
    cursor.execute('SET CHARACTER SET utf8mb4;')
    cursor.execute('SET character_set_connection=utf8mb4;')
    print('connected to database...')

def closeDB():
    global cursor
    global mariadb_connection
    mariadb_connection.commit()
    cursor.close()
    mariadb_connection.close()

def commandLineOptions():
    if len(sys.argv) < 4:
        print("Usage: " + sys.argv[0] + " <database host> <db user> <db password>")
        sys.exit(1)
    else:
        print("host : " + sys.argv[1] + " !")
        print("user : " + sys.argv[2] + " !")
        print("password : " + sys.argv[3] + " !")
        global db_host
        db_host = sys.argv[1]
        global db_user
        db_user = sys.argv[2]
        global db_password
        db_password = sys.argv[3]

def convert_p_register():
    global cursor
    sql = 'SELECT id, address_offset, reset_value, reset_mask FROM p_register'
    cursor.execute(sql)
    result = cursor.fetchall()
    fixed = 0
    for row in result:
        id = row[0]
        offset = row[1]
        val = row[2]
        mask = row[3]

        if None == offset:
            offset = "0"
        offstr = hex(int(offset))
        offstr = offstr[2:]
        offstr = "0x" + offstr.upper()

        if None == val:
            val = "0"
        valstr = hex(int(val))
        valstr = valstr[2:]
        valstr = "0x" + valstr.upper()

        if None == mask:
            mask = "0"
        maskstr = hex(int(mask))
        maskstr = maskstr[2:]
        maskstr = "0x" + maskstr.upper()

        sql = 'UPDATE p_register SET address_offset_str = "' + str(offstr) + '", reset_value_str = "' + str(valstr) + '", reset_mask_str = "' + str(maskstr) + '" WHERE id = ' + str(id)
        #print(str(sql))
        cursor.execute(sql)
        fixed += 1
    print('fixed ' + str(fixed))

def convert_pPeripheralInstance():
    global cursor
    sql = 'SELECT id, base_address FROM p_peripheral_instance'
    cursor.execute(sql)
    result = cursor.fetchall()
    fixed = 0
    for row in result:
        id = row[0]
        offset = row[1]

        if None == offset:
            offset = "0"
        offstr = hex(int(offset))
        offstr = offstr[2:]
        offstr = "0x" + offstr.upper()

        sql = 'UPDATE p_peripheral_instance SET base_address_str = "' + str(offstr) + '" WHERE id = ' + str(id)
        #print(str(sql))
        cursor.execute(sql)
        fixed += 1
    print('fixed ' + str(fixed))

def convert_p_field():
    global cursor
    sql = 'SELECT id, reset_value FROM p_field'
    cursor.execute(sql)
    result = cursor.fetchall()
    fixed = 0
    for row in result:
        id = row[0]
        offset = row[1]

        if None == offset:
            offset = "0"
        offstr = hex(int(offset))
        offstr = offstr[2:]
        offstr = "0x" + offstr.upper()

        sql = 'UPDATE p_field SET reset_value_str = "' + str(offstr) + '" WHERE id = ' + str(id)
        #print(str(sql))
        cursor.execute(sql)
        fixed += 1
    print('fixed ' + str(fixed))

def convert_p_address_block():
    global cursor
    sql = 'SELECT id, address_offset, size FROM p_address_block'
    cursor.execute(sql)
    result = cursor.fetchall()
    fixed = 0
    for row in result:
        id = row[0]
        offset = row[1]
        val = row[2]

        if None == offset:
            offset = "0"
        offstr = hex(int(offset))
        offstr = offstr[2:]
        offstr = "0x" + offstr.upper()

        if None == val:
            val = "0"
        valstr = hex(int(val))
        valstr = valstr[2:]
        valstr = "0x" + valstr.upper()

        sql = 'UPDATE p_address_block SET address_offset_str = "' + str(offstr) + '", size_str = "' + str(valstr) + '" WHERE id = ' + str(id)
        #print(str(sql))
        cursor.execute(sql)
        fixed += 1
    print('fixed ' + str(fixed))

def remove_p_enumeration():
    global cursor
    sql = 'SELECT field_id, enum_id FROM pl_enumeration'
    cursor.execute(sql)
    result = cursor.fetchall()
    fixed = 0
    enums = 0
    values = 0;
    for row in result:
        field_id = row[0]
        enum_id = row[1]
        sql = 'SELECT name, usage_right FROM p_enumeration WHERE id = ' + str(enum_id)
        cursor.execute(sql)
        res_pe = cursor.fetchall()
        num_enum = 0;
        for row_pe in res_pe:
            num_enum = num_enum + 1
            if 2 == num_enum:
                exit(2)
            enums += 1
            enum_name = row_pe[0]
            enum_usage_right = row_pe[1]
            sql = 'UPDATE p_field SET is_Enum = true, enum_name = "' + str(enum_name) + '", enum_usage_right = "' + str(enum_usage_right) + '" WHERE id = ' + str(field_id)
            cursor.execute(sql)
            # get elements
            sql = 'SELECT value_id FROM pl_enumeration_element WHERE enum_id = ' + str(enum_id)
            cursor.execute(sql)
            res_ev = cursor.fetchall()
            for row_ev in res_ev:
                value_id = row_ev[0]
                sql = 'INSERT INTO pl_enum_element (field_id, enum_element_id) VALUES (' + str(field_id) + ', ' + str(value_id) + ')'
                cursor.execute(sql)
                values +=1
            fixed += 1
    print('found ' + str(enums) + ' enums')
    print('found ' + str(values) + ' enum values')
    print('fixed ' + str(fixed))


def removeNullAsSvdId():
    print('Changing:')
    num = 0
    sql = 'SELECT svd_id, name, id FROM microcontroller'
    rows = cursor.execute(sql)
    print('found ' + str(rows) + ' rows!')
    result = cursor.fetchall()
    for row in result:
        if None == row[0]:
            sql = 'UPDATE microcontroller SET svd_id = 0 WHERE id = ' + str(row[2])
            res = cursor.execute(sql)
            if 0 == res:
                print('ERROR: (' + str(row[1]) + ') id ' + str(row[2]))
            elif 1 == res:
                pass
    print('Done ' + str(num) + ' checks!')


if __name__ == '__main__':
    commandLineOptions()
    connect2localDB()
    #convert_p_register()
    #convert_pPeripheralInstance()
    #convert_p_field()
    #convert_p_address_block()
    #remove_p_enumeration()
    removeNullAsSvdId()
    closeDB()
    print('Done !')