resource "aws_lb_listener" "vulnerable_example" {
  load_balancer_arn = "arn:aws:iam::123456789012:user/johndoe"
  port              = "443"
  protocol          = "HTTP"
  ssl_policy        = "ELBSecurityPolicy-2016-08"
  certificate_arn   = "arn:aws:iam::187416307283:server-certificate/test_cert_rab3wuqwgja25ct3n4jdj2tzu4"

  default_action {
    type             = "forward"
    target_group_arn = "arn:aws:iam::123456789012:user/johndoe"
  }
}