package com.techacademy.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.techacademy.constants.ErrorKinds;
import com.techacademy.constants.ErrorMessage;
import com.techacademy.entity.Employee;
import com.techacademy.service.EmployeeService;
import com.techacademy.service.UserDetail;

@Controller
@RequestMapping("employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // 従業員一覧画面
    @GetMapping
    public String list(Model model) {
        model.addAttribute("listSize", employeeService.findAll().size());
        model.addAttribute("employeeList", employeeService.findAll());
        return "employees/list";
    }

    @GetMapping(value = "/{code}/")
    public String detail(@PathVariable("code") String code, Model model) {
        Employee employee = employeeService.findByCode(code);

        if (employee == null) {
            model.addAttribute("errorMessage", "従業員が見つかりませんでした");
            return "error"; // エラーページを表示
        }


        model.addAttribute("employee", employee);
        return "employees/detail";
    }




    // 従業員新規登録画面
    @GetMapping(value = "/add")
    public String create(Model model) {
        model.addAttribute("employee", new Employee());
        return "employees/new";
    }

    // 従業員新規登録処理
    @PostMapping(value = "/add")
    public String add(@Validated @ModelAttribute Employee employee, BindingResult result, Model model) {
        if (employee.getPassword() == null || employee.getPassword().isEmpty()) {
            model.addAttribute(ErrorMessage.getErrorName(ErrorKinds.BLANK_ERROR),
                    ErrorMessage.getErrorValue(ErrorKinds.BLANK_ERROR));
            return create(model);
        }

        if (result.hasErrors()) {
            return create(model);
        }

        try {
            ErrorKinds saveResult = employeeService.register(employee);
            if (saveResult != ErrorKinds.SUCCESS) {
                model.addAttribute("errorMessage", "登録処理中にエラーが発生しました");
                return create(model);
            }
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "入力された従業員情報が重複しています");
            return create(model);
        }

        return "redirect:/employees";
    }

    // 従業員更新画面表示
    @GetMapping(value = "/{code}/update")
    public String update(@PathVariable("code") String code, Model model) {
        model.addAttribute("employee", employeeService.findByCode(code));
        model.addAttribute("roles", List.of("一般", "管理者"));
        return "employees/update";
    }

    // 従業員更新処理
    @PostMapping(value = "/{code}/update")
    public String update(@PathVariable("code") String code, @Validated @ModelAttribute Employee employee, BindingResult result, Model model) {
        System.out.println("POSTリクエスト受信: code=" + code);

        if (result.hasErrors()) {
            model.addAttribute("employee", employee);
            return "employees/update";
        }

        try {
            ErrorKinds updateResult = employeeService.update(employee); // 修正: save() → update()
            if (updateResult != ErrorKinds.SUCCESS) {
                System.out.println("エラー: " + updateResult);
                model.addAttribute("errorMessage", "更新処理中にエラーが発生しました");
                return "employees/update";
            }
        } catch (Exception e) {
            System.out.println("エラー: " + e.getMessage());
            model.addAttribute("errorMessage", "更新処理中に例外が発生しました");
            return "employees/update";
        }

        System.out.println("更新成功");
        return "redirect:/employees";
    }


    // 従業員削除処
    @PostMapping(value = "/{code}/delete")
    public String delete(@PathVariable("code") String code, @AuthenticationPrincipal UserDetail userDetail, Model model) {
        ErrorKinds deleteResult = employeeService.delete(code);

        if (deleteResult != ErrorKinds.SUCCESS) {
            model.addAttribute("errorMessage", "削除処理中にエラーが発生しました");
            model.addAttribute("employee", employeeService.findByCode(code));
            return detail(code, model);
        }

        return "redirect:/employees";
    }
}

