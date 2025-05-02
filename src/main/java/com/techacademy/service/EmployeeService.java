package com.techacademy.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.techacademy.constants.ErrorKinds;
import com.techacademy.entity.Employee;
import com.techacademy.repository.EmployeeRepository;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 従業員情報の新規登録
    @Transactional
    public ErrorKinds register(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee オブジェクトが null です");
        }

        // パスワードチェック
        ErrorKinds result = employeePasswordCheck(employee.getPassword());
        if (result != ErrorKinds.CHECK_OK) {
            return result;
        }

        employee.setDeleteFlg(false);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());
        employee.setPassword(passwordEncoder.encode(employee.getPassword())); // パスワード暗号化
        employeeRepository.save(employee);

        return ErrorKinds.SUCCESS;
    }

    // 従業員情報の更新（パスワードが空でも動作）
    @Transactional
    public ErrorKinds update(Employee employee) {
        if (employee == null || employee.getCode() == null) {
            return ErrorKinds.NOT_FOUND_ERROR;
        }

        Employee existingEmployee = findByCode(employee.getCode());
        if (existingEmployee == null) {
            return ErrorKinds.NOT_FOUND_ERROR;
        }

        // 氏名を更新
        existingEmployee.setName(employee.getName());

        // パスワードが入力されていれば更新（空ならスキップ）
        if (employee.getPassword() != null && !employee.getPassword().isEmpty()) {
            existingEmployee.setPassword(passwordEncoder.encode(employee.getPassword()));
        }

        existingEmployee.setUpdatedAt(LocalDateTime.now());
        employeeRepository.save(existingEmployee);

        return ErrorKinds.SUCCESS;
    }

    // 従業員削除
    @Transactional
    public ErrorKinds delete(String code) {
        Employee employee = findByCode(code);
        if (employee == null) {
            return ErrorKinds.NOT_FOUND_ERROR;
        }

        employee.setDeleteFlg(true);
        employee.setUpdatedAt(LocalDateTime.now());
        employeeRepository.save(employee);

        return ErrorKinds.SUCCESS;
    }

    // 従業員一覧取得
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    // 従業員を1件取得
    public Employee findByCode(String code) {
        return employeeRepository.findById(code).orElse(null);
    }

    // パスワードのバリデーションチェック
    private ErrorKinds employeePasswordCheck(String password) {
        if (password == null || password.isEmpty()) {
            return ErrorKinds.BLANK_ERROR;
        }
        if (isHalfSizeCheckError(password)) {
            return ErrorKinds.HALFSIZE_ERROR;
        }
        if (isOutOfRangePassword(password)) {
            return ErrorKinds.RANGECHECK_ERROR;
        }
        return ErrorKinds.CHECK_OK;
    }

    // 半角英数字チェック
    private boolean isHalfSizeCheckError(String password) {
        return !Pattern.compile("^[A-Za-z0-9]+$").matcher(password).matches();
    }

    // 文字数チェック（8文字～16文字）
    private boolean isOutOfRangePassword(String password) {
        return password.length() < 8 || password.length() > 16;
    }
}
